package lila.security

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraints
import play.api.mvc.RequestHeader

import lila.common.{ EmailAddress, Form as LilaForm, LameName }
import lila.common.Form.{ *, given }
import lila.user.User.{ ClearPassword, TotpToken }
import lila.user.{ TotpSecret, User, UserRepo }

final class SecurityForm(
    userRepo: UserRepo,
    authenticator: lila.user.Authenticator,
    emailValidator: EmailAddressValidator,
    lameNameCheck: LameNameCheck,
    hcaptcha: Hcaptcha
)(using Executor):

  import SecurityForm.*

  private val passwordMinLength = 4

  private val anyEmail: Mapping[EmailAddress] =
    LilaForm
      .cleanNonEmptyText(minLength = 6, maxLength = EmailAddress.maxLength)
      .verifying(Constraints.emailAddress)
      .into[EmailAddress]

  private val sendableEmail = anyEmail.verifying(emailValidator.sendableConstraint)

  private def fullyValidEmail(forUser: Option[User]) = sendableEmail
    .verifying(emailValidator.withAcceptableDns)
    .verifying(emailValidator.uniqueConstraint(forUser))

  private val preloadEmailDnsForm = Form(single("email" -> sendableEmail))

  def preloadEmailDns()(using req: play.api.mvc.Request[?], formBinding: FormBinding): Funit =
    preloadEmailDnsForm
      .bindFromRequest()
      .fold(_ => funit, emailValidator.preloadDns)

  object signup:

    val emailField = fullyValidEmail(none)

    val username = LilaForm.cleanNonEmptyText
      .verifying(
        Constraints minLength 2,
        Constraints maxLength 20,
        Constraints.pattern(
          regex = User.newUsernamePrefix,
          error = "usernamePrefixInvalid"
        ),
        Constraints.pattern(
          regex = User.newUsernameSuffix,
          error = "usernameSuffixInvalid"
        ),
        Constraints.pattern(
          regex = User.newUsernameChars,
          error = "usernameCharsInvalid"
        ),
        Constraints.pattern(
          regex = User.newUsernameLetters,
          error = "usernameCharsInvalid"
        )
      )
      .into[UserName]
      .verifying("usernameUnacceptable", u => !lameNameCheck.value || !LameName.username(u))
      .verifying(
        "usernameAlreadyUsed",
        u => !User.isGhost(u.id) && !userRepo.exists(u).await(3 seconds, "signupUsername")
      )

    private val agreementBool = boolean.verifying(b => b)

    private val agreement = mapping(
      "assistance" -> agreementBool,
      "nice"       -> agreementBool,
      "account"    -> agreementBool,
      "policy"     -> agreementBool
    )(AgreementData.apply)(unapply)

    def website(implicit req: RequestHeader) = hcaptcha.form(
      Form(
        mapping(
          "username"  -> username,
          "password"  -> text(minLength = passwordMinLength),
          "email"     -> emailField,
          "agreement" -> agreement,
          "fp"        -> optional(nonEmptyText)
        )(SignupData.apply)(_ => None)
      )
    )

    val mobile = Form(
      mapping(
        "username" -> username,
        "password" -> text(minLength = passwordMinLength),
        "email"    -> emailField
      )(MobileSignupData.apply)(_ => None)
    )

  def passwordReset(implicit req: RequestHeader) = hcaptcha.form(
    Form(
      mapping(
        "email" -> sendableEmail // allow unacceptable emails for BC
      )(PasswordReset.apply)(_ => None)
    )
  )

  val newPassword = Form(
    single(
      "password" -> text(minLength = passwordMinLength)
    )
  )

  case class PasswordResetConfirm(newPasswd1: String, newPasswd2: String):
    def samePasswords = newPasswd1 == newPasswd2

  val passwdReset = Form(
    mapping(
      "newPasswd1" -> nonEmptyText(minLength = passwordMinLength),
      "newPasswd2" -> nonEmptyText(minLength = passwordMinLength)
    )(PasswordResetConfirm.apply)(unapply).verifying(
      "newPasswordsDontMatch",
      _.samePasswords
    )
  )

  def magicLink(implicit req: RequestHeader) = hcaptcha.form(
    Form(
      mapping(
        "email" -> sendableEmail // allow unacceptable emails for BC
      )(MagicLink.apply)(_ => None)
    )
  )

  def changeEmail(u: User, old: Option[EmailAddress]) =
    authenticator loginCandidate u map { candidate =>
      Form(
        mapping(
          "passwd" -> passwordMapping(candidate),
          "email"  -> fullyValidEmail(candidate.user.some).verifying(emailValidator differentConstraint old)
        )(ChangeEmail.apply)(unapply)
      ).fillOption(old.map { ChangeEmail("", _) })
    }

  def setupTwoFactor(u: User) =
    authenticator loginCandidate u map { candidate =>
      Form(
        mapping(
          "secret" -> nonEmptyText,
          "passwd" -> passwordMapping(candidate),
          "token"  -> nonEmptyText
        )(TwoFactor.apply)(unapply).verifying(
          "invalidAuthenticationCode",
          _.tokenValid
        )
      ).fill(
        TwoFactor(
          secret = TotpSecret.random.base32,
          passwd = "",
          token = ""
        )
      )
    }

  def disableTwoFactor(u: User) =
    authenticator loginCandidate u map { candidate =>
      Form(
        tuple(
          "passwd" -> passwordMapping(candidate),
          "token" -> text.verifying("invalidAuthenticationCode", t => u.totpSecret.??(_.verify(TotpToken(t))))
        )
      )
    }

  def fixEmail(old: EmailAddress) =
    Form(
      single("email" -> fullyValidEmail(none).verifying(emailValidator differentConstraint old.some))
    ).fill(old)

  def modEmail(user: User) = Form(
    single("email" -> anyEmail.verifying(emailValidator uniqueConstraint user.some))
  )

  private def passwordProtected(u: User) =
    authenticator loginCandidate u map { candidate =>
      Form(single("passwd" -> passwordMapping(candidate)))
    }

  def closeAccount = passwordProtected

  def toggleKid = passwordProtected

  def reopen(implicit req: RequestHeader) = hcaptcha.form(
    Form(
      mapping(
        "username" -> LilaForm.cleanNonEmptyText.into[UserStr],
        "email"    -> sendableEmail // allow unacceptable emails for BC
      )(Reopen.apply)(_ => None)
    )
  )

  private def passwordMapping(candidate: User.LoginCandidate) =
    text.verifying("incorrectPassword", p => candidate.check(ClearPassword(p)))

object SecurityForm:

  case class AgreementData(
      assistance: Boolean,
      nice: Boolean,
      account: Boolean,
      policy: Boolean
  )

  case class SignupData(
      username: UserName,
      password: String,
      email: EmailAddress,
      agreement: AgreementData,
      fp: Option[String]
  ):
    def fingerPrint = FingerPrint from fp.filter(_.nonEmpty)

  case class MobileSignupData(
      username: UserName,
      password: String,
      email: EmailAddress
  )

  case class PasswordReset(email: EmailAddress)

  case class MagicLink(email: EmailAddress)

  case class Reopen(username: UserStr, email: EmailAddress)

  case class ChangeEmail(passwd: String, email: EmailAddress)

  case class TwoFactor(secret: String, passwd: String, token: String):
    def tokenValid = TotpSecret(secret).verify(User.TotpToken(token))
