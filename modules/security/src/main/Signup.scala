package lila.security

import play.api.data.*
import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }
import scala.util.chaining.*

import lila.common.config.NetConfig
import lila.common.{ ApiVersion, EmailAddress, HTTPRequest, IpAddress }
import lila.memo.RateLimit
import lila.user.{ PasswordHasher, User }

final class Signup(
    store: Store,
    api: SecurityApi,
    ipTrust: IpTrust,
    forms: SecurityForm,
    emailAddressValidator: EmailAddressValidator,
    emailConfirm: EmailConfirm,
    hcaptcha: Hcaptcha,
    authenticator: lila.user.Authenticator,
    userRepo: lila.user.UserRepo,
    irc: lila.irc.IrcApi,
    disposableEmailAttempt: DisposableEmailAttempt,
    netConfig: NetConfig
)(using Executor):

  private enum MustConfirmEmail(val value: Boolean):
    case Nope                   extends MustConfirmEmail(false)
    case YesBecausePrintExists  extends MustConfirmEmail(true)
    case YesBecausePrintMissing extends MustConfirmEmail(true)
    case YesBecauseIpExists     extends MustConfirmEmail(true)
    case YesBecauseIpSusp       extends MustConfirmEmail(true)
    case YesBecauseMobile       extends MustConfirmEmail(true)
    case YesBecauseUA           extends MustConfirmEmail(true)
    case YesBecauseEmailDomain  extends MustConfirmEmail(true)

  private object MustConfirmEmail:
    def apply(print: Option[FingerPrint], email: EmailAddress)(using
        req: RequestHeader
    ): Fu[MustConfirmEmail] =
      val ip = HTTPRequest ipAddress req
      store.recentByIpExists(ip, 7.days) flatMap { ipExists =>
        if (ipExists) fuccess(YesBecauseIpExists)
        else if (HTTPRequest weirdUA req) fuccess(YesBecauseUA)
        else
          print.fold[Fu[MustConfirmEmail]](fuccess(YesBecausePrintMissing)) { fp =>
            store.recentByPrintExists(fp) flatMap { printFound =>
              if (printFound) fuccess(YesBecausePrintExists)
              else
                ipTrust.isSuspicious(ip).map {
                  case true => YesBecauseIpSusp
                  case _ =>
                    if (email.domain.map(_.lower) exists DisposableEmailDomain.whitelisted) Nope
                    else YesBecauseEmailDomain
                }
            }
          }
      }

  def website(
      blind: Boolean
  )(using req: Request[?], lang: Lang, formBinding: FormBinding): Fu[Signup.Result] =
    forms.signup.website flatMap {
      _.form
        .bindFromRequest()
        .fold[Fu[Signup.Result]](
          err =>
            fuccess {
              disposableEmailAttempt.onFail(err, HTTPRequest ipAddress req)
              Signup.Result.Bad(err tap signupErrLog)
            },
          data =>
            hcaptcha.verify().flatMap {
              case Hcaptcha.Result.Fail           => fuccess(Signup.Result.MissingCaptcha)
              case Hcaptcha.Result.Pass if !blind => fuccess(Signup.Result.MissingCaptcha)
              case hcaptchaResult =>
                signupRateLimit(data.username.id, if (hcaptchaResult == Hcaptcha.Result.Valid) 1 else 2) {
                  MustConfirmEmail(data.fingerPrint, data.email) flatMap { mustConfirm =>
                    lila.mon.user.register.count(none)
                    lila.mon.user.register.mustConfirmEmail(mustConfirm.toString).increment()
                    val passwordHash = authenticator passEnc data.clearPassword
                    userRepo
                      .create(
                        data.username,
                        passwordHash,
                        data.email,
                        blind,
                        none,
                        mustConfirmEmail = mustConfirm.value
                      )
                      .orFail(s"No user could be created for ${data.username}")
                      .addEffect { logSignup(req, _, data.email, data.fingerPrint, none, mustConfirm) }
                      .flatMap {
                        confirmOrAllSet(data.email, mustConfirm, data.fingerPrint, none)
                      }
                  }
                }
            }
        )
    }

  private def confirmOrAllSet(
      email: EmailAddress,
      mustConfirm: MustConfirmEmail,
      fingerPrint: Option[FingerPrint],
      apiVersion: Option[ApiVersion]
  )(user: User)(implicit req: RequestHeader, lang: Lang): Fu[Signup.Result] =
    store.deletePreviousSessions(user) >> {
      if (mustConfirm.value)
        emailConfirm.send(user, email) >> {
          if (emailConfirm.effective)
            api.saveSignup(user.id, apiVersion, fingerPrint) inject
              Signup.Result.ConfirmEmail(user, email)
          else fuccess(Signup.Result.AllSet(user, email))
        }
      else fuccess(Signup.Result.AllSet(user, email))
    }

  def mobile(
      apiVersion: ApiVersion
  )(implicit req: Request[?], lang: Lang, formBinding: FormBinding): Fu[Signup.Result] =
    forms.signup.mobile
      .bindFromRequest()
      .fold[Fu[Signup.Result]](
        err =>
          fuccess {
            disposableEmailAttempt.onFail(err, HTTPRequest ipAddress req)
            Signup.Result.Bad(err tap signupErrLog)
          },
        data =>
          signupRateLimit(data.username.id, cost = 2) {
            val mustConfirm = MustConfirmEmail.YesBecauseMobile
            lila.mon.user.register.count(apiVersion.some).increment()
            lila.mon.user.register.mustConfirmEmail(mustConfirm.toString).increment()
            val passwordHash = authenticator passEnc User.ClearPassword(data.password)
            userRepo
              .create(
                data.username,
                passwordHash,
                data.email,
                blind = false,
                apiVersion.some,
                mustConfirmEmail = mustConfirm.value
              )
              .orFail(s"No user could be created for ${data.username}")
              .addEffect { logSignup(req, _, data.email, none, apiVersion.some, mustConfirm) }
              .flatMap {
                confirmOrAllSet(data.email, mustConfirm, none, apiVersion.some)
              }
          }
      )

  private def HasherRateLimit =
    PasswordHasher.rateLimit[Signup.Result](enforce = netConfig.rateLimit, ipCost = 1)

  private lazy val signupRateLimitPerIP = RateLimit.composite[IpAddress](
    key = "account.create.ip",
    enforce = netConfig.rateLimit.value
  )(
    ("fast", 10, 10.minutes),
    ("slow", 150, 1 day)
  )

  private val rateLimitDefault = fuccess(Signup.Result.RateLimited)

  private def signupRateLimit(id: UserId, cost: Int)(
      f: => Fu[Signup.Result]
  )(implicit req: RequestHeader): Fu[Signup.Result] =
    HasherRateLimit(id, req) { _ =>
      signupRateLimitPerIP(HTTPRequest ipAddress req, cost = cost)(f)(rateLimitDefault)
    }(rateLimitDefault)

  private def logSignup(
      req: RequestHeader,
      user: User,
      email: EmailAddress,
      fingerPrint: Option[FingerPrint],
      apiVersion: Option[ApiVersion],
      mustConfirm: MustConfirmEmail
  ) =
    disposableEmailAttempt.onSuccess(user, email, HTTPRequest ipAddress req)
    authLog(
      user.username into UserStr,
      email.value,
      s"fp: $fingerPrint mustConfirm: $mustConfirm fp: ${fingerPrint
          .??(_.value)} ip: ${HTTPRequest ipAddress req} api: $apiVersion"
    )

  private def signupErrLog(err: Form[?]) =
    for {
      username <- err("username").value
      email    <- err("email").value
    }
      if (
        err.errors.exists(_.messages.contains("error.email_acceptable")) &&
        err("email").value.exists(EmailAddress.isValid)
      )
        authLog(UserStr(username), email, "Signup with unacceptable email")

  private def authLog(user: UserStr, email: String, msg: String) =
    lila.log("auth").info(s"$user $email $msg")

object Signup:

  enum Result:
    case Bad(err: Form[?])
    case MissingCaptcha
    case RateLimited
    case ConfirmEmail(user: User, email: EmailAddress)
    case AllSet(user: User, email: EmailAddress)
