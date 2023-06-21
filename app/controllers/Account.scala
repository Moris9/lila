package controllers

import play.api.libs.json.*
import play.api.mvc.*
import play.api.data.Form
import scala.util.chaining.*
import views.html
import scalatags.Text.Frag

import lila.api.AnnounceStore
import lila.app.{ given, * }
import lila.security.SecurityForm.Reopen
import lila.user.{ TotpSecret, User as UserModel }
import lila.i18n.I18nLangPicker

final class Account(
    env: Env,
    auth: Auth,
    apiC: => Api
) extends LilaController(env):

  def profile = Auth { _ ?=> me ?=>
    Ok.page:
      html.account.profile(me, env.user.forms profileOf me)
  }

  def username = Auth { _ ?=> me ?=>
    Ok.page:
      html.account.username(me, env.user.forms usernameOf me)
  }

  def profileApply = AuthBody { _ ?=> me ?=>
    FormFuResult(env.user.forms.profile)(err => renderPage(html.account.profile(me, err))): profile =>
      profile.bio
        .exists(env.security.spam.detect)
        .option("profile.bio" -> ~profile.bio)
        .orElse:
          profile.links
            .exists(env.security.spam.detect)
            .option("profile.links" -> ~profile.links)
        .so { (resource, text) =>
          env.report.api.autoCommFlag(lila.report.Suspect(me).id, resource, text)
        } >> env.user.repo.setProfile(me, profile) >>
        Redirect(routes.User show me.username).flashSuccess
  }

  def usernameApply = AuthBody { _ ?=> me ?=>
    FormFuResult(env.user.forms.username(me))(err => renderPage(html.account.username(me, err))): username =>
      env.user.repo.setUsernameCased(me, username) inject
        Redirect(routes.User show me.username).flashSuccess recover { case e =>
          Redirect(routes.Account.username).flashFailure(e.getMessage)
        }
  }

  def info = Auth { _ ?=> me ?=>
    negotiate(
      html = notFound,
      api = _ =>
        for
          povs         <- env.round.proxyRepo urgentGames me
          nbChallenges <- env.challenge.api.countInFor get me
          playban      <- env.playban.api currentBan me
        yield Ok {
          import lila.pref.JsonView.given
          env.user.jsonView
            .full(me, withRating = ctx.pref.showRatings, withProfile = false) ++ Json
            .obj(
              "prefs"        -> ctx.pref,
              "nowPlaying"   -> JsArray(povs take 50 map env.api.lobbyApi.nowPlaying),
              "nbChallenges" -> nbChallenges,
              "online"       -> true
            )
            .add("kid" -> me.kid)
            .add("troll" -> me.marks.troll)
            .add("playban" -> playban)
            .add("announce" -> AnnounceStore.get.map(_.json))
        }.withHeaders(CACHE_CONTROL -> "max-age=15")
    )
  }

  def nowPlaying = Auth { _ ?=> _ ?=>
    negotiate(
      html = notFound,
      api = _ => doNowPlaying
    )
  }

  val apiMe =
    val rateLimit = lila.memo.RateLimit[UserId](30, 10.minutes, "api.account.user")
    Scoped() { ctx ?=> me ?=>
      def limited = rateLimitedFu:
        "Please don't poll this endpoint. Stream https://lichess.org/api#tag/Board/operation/apiStreamEvent instead."
      rateLimit(me, limited):
        env.api.userApi.extended(
          me,
          me.some,
          withFollows = apiC.userWithFollows,
          withTrophies = false
        ) dmap { JsonOk(_) }
    }

  def apiNowPlaying = Scoped()(doNowPlaying)

  private def doNowPlaying(using WebContext)(using me: Me) =
    env.round.proxyRepo.urgentGames(me) map { povs =>
      val nb = (getInt("nb") | 9) atMost 50
      Ok(Json.obj("nowPlaying" -> JsArray(povs take nb map env.api.lobbyApi.nowPlaying)))
    }

  def dasher = Auth { _ ?=> me ?=>
    negotiate(
      html = notFound,
      api = _ =>
        env.pref.api.get(me).map { prefs =>
          Ok:
            import lila.pref.JsonView.given
            lila.common.LightUser.lightUserWrites.writes(me.light) ++ Json.obj(
              "coach" -> isGranted(_.Coach),
              "prefs" -> prefs
            )
        }
    )
  }

  def passwd = Auth { _ ?=> me ?=>
    env.security.forms.passwdChange.flatMap: form =>
      Ok.page(html.account.passwd(form))
  }

  def passwdApply = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.passwdChange.flatMap: form =>
        FormFuResult(form)(err => renderPage(html.account.passwd(err))): data =>
          env.user.authenticator.setPassword(me, UserModel.ClearPassword(data.newPasswd1)) >>
            refreshSessionId(Redirect(routes.Account.passwd).flashSuccess)
  }

  private def refreshSessionId(result: Result)(using ctx: WebContext, me: Me): Fu[Result] =
    env.security.store.closeAllSessionsOf(me) >>
      env.push.webSubscriptionApi.unsubscribeByUser(me) >>
      env.push.unregisterDevices(me) >>
      env.security.api.saveAuthentication(me, ctx.mobileApiVersion) map { sessionId =>
        result.withCookies(env.lilaCookie.session(env.security.api.sessionIdKey, sessionId))
      }

  private def emailForm(using me: Me) =
    env.user.repo email me flatMap env.security.forms.changeEmail

  def email = Auth { _ ?=> me ?=>
    if getBool("check")
    then Ok async renderCheckYourEmail
    else emailForm.flatMap(f => Ok.page(html.account.email(f)))
  }

  def apiEmail = Scoped(_.Email.Read) { _ ?=> me ?=>
    env.user.repo email me mapz { email =>
      JsonOk(Json.obj("email" -> email.value))
    }
  }

  def renderCheckYourEmail(using WebContext): Fu[Frag] =
    renderPage:
      html.auth.checkYourEmail(lila.security.EmailConfirm.cookie get ctx.req)

  def emailApply = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.preloadEmailDns() >> emailForm.flatMap { form =>
        FormFuResult(form)(err => renderPage(html.account.email(err))): data =>
          val newUserEmail = lila.security.EmailConfirm.UserEmail(me.username, data.email)
          auth.EmailConfirmRateLimit(newUserEmail, ctx.req, rateLimitedFu):
            env.security.emailChange.send(me, newUserEmail.email) inject
              Redirect(routes.Account.email).flashSuccess:
                lila.i18n.I18nKeys.checkYourEmail.txt()
      }
  }

  def emailConfirm(token: String) = Open:
    env.security.emailChange.confirm(token) flatMapz { (user, prevEmail) =>
      (prevEmail.exists(_.isNoReply) so env.clas.api.student.release(user)) >>
        auth.authenticateUser(
          user,
          remember = true,
          result =
            if (prevEmail.exists(_.isNoReply))
              Some(_ => Redirect(routes.User.show(user.username)).flashSuccess)
            else
              Some(_ => Redirect(routes.Account.email).flashSuccess)
        )
    }

  def emailConfirmHelp = OpenBody:
    import lila.security.EmailConfirm.Help.*
    ctx.me match
      case Some(me) =>
        Redirect(routes.User.show(me.username))
      case None if get("username").isEmpty =>
        Ok.page(html.account.emailConfirmHelp(helpForm, none))
      case None =>
        helpForm
          .bindFromRequest()
          .fold(
            err => BadRequest.page(html.account.emailConfirmHelp(err, none)),
            username =>
              getStatus(env.user.repo, username).flatMap: status =>
                Ok.page(html.account.emailConfirmHelp(helpForm fill username, status.some))
          )

  def twoFactor = Auth { _ ?=> me ?=>
    if me.totpSecret.isDefined
    then
      env.security.forms.disableTwoFactor(me).flatMap { f =>
        Ok.page(html.account.twoFactor.disable(f))
      }
    else
      env.security.forms.setupTwoFactor(me) flatMap { f =>
        Ok.page(html.account.twoFactor.setup(f))
      }

  }

  def setupTwoFactor = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.setupTwoFactor(me) flatMap { form =>
        FormFuResult(form)(err => renderPage(html.account.twoFactor.setup(err))): data =>
          env.user.repo.setupTwoFactor(me, TotpSecret(data.secret)) >>
            refreshSessionId(Redirect(routes.Account.twoFactor).flashSuccess)
      }
  }

  def disableTwoFactor = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.disableTwoFactor(me) flatMap { form =>
        FormFuResult(form)(err => renderPage(html.account.twoFactor.disable(err))): _ =>
          env.user.repo.disableTwoFactor(me) inject
            Redirect(routes.Account.twoFactor).flashSuccess
      }
  }

  def close = Auth { _ ?=> me ?=>
    env.clas.api.student.isManaged(me) flatMap { managed =>
      env.security.forms.closeAccount.flatMap: form =>
        Ok.page(html.account.close(form, managed))
    }
  }

  def closeConfirm = AuthBody { ctx ?=> me ?=>
    NotManaged:
      auth.HasherRateLimit:
        env.security.forms.closeAccount.flatMap: form =>
          FormFuResult(form)(err => renderPage(html.account.close(err, managed = false))): _ =>
            env.api.accountClosure
              .close(me.user)
              .inject:
                Redirect(routes.User show me.username) withCookies env.lilaCookie.newSession
  }

  def kid = Auth { _ ?=> me ?=>
    for
      managed <- env.clas.api.student.isManaged(me)
      form    <- env.security.forms.toggleKid
      page    <- Ok.page(html.account.kid(me, form, managed))
    yield page
  }
  def apiKid = Scoped(_.Preference.Read) { _ ?=> me ?=>
    JsonOk(Json.obj("kid" -> me.kid))
  }

  def kidPost = AuthBody { ctx ?=> me ?=>
    NotManaged:
      env.security.forms.toggleKid.flatMap: form =>
        form
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                html = BadRequest.page(html.account.kid(me, err, managed = false)),
                api = _ => BadRequest(errorsAsJson(err))
              ),
            _ =>
              env.user.repo.setKid(me, getBool("v")) >>
                negotiate(
                  html = Redirect(routes.Account.kid).flashSuccess,
                  api = _ => jsonOkResult
                )
          )
  }

  def apiKidPost = Scoped(_.Preference.Write) { ctx ?=> me ?=>
    getBoolOpt("v") match
      case None    => BadRequest(jsonError("Missing v parameter"))
      case Some(v) => env.user.repo.setKid(me, v) inject jsonOkResult
  }

  private def currentSessionId(using WebContext) =
    ~env.security.api.reqSessionId(ctx.req)

  def security = Auth { _ ?=> me ?=>
    for
      _                    <- env.security.api.dedup(me, ctx.req)
      sessions             <- env.security.api.locatedOpenSessions(me, 50)
      clients              <- env.oAuth.tokenApi.listClients(me, 50)
      personalAccessTokens <- env.oAuth.tokenApi.countPersonal(me)
      page <- renderPage:
        html.account.security(me, sessions, currentSessionId, clients, personalAccessTokens)
    yield Ok(page)
  }

  def signout(sessionId: String) = Auth { _ ?=> me ?=>
    if sessionId == "all"
    then refreshSessionId(Redirect(routes.Account.security).flashSuccess)
    else
      env.security.store.closeUserAndSessionId(me, sessionId) >>
        env.push.webSubscriptionApi.unsubscribeBySession(sessionId)
  }

  private def renderReopen(form: Option[Form[Reopen]], msg: Option[String])(using
      WebContext
  ): Fu[Frag] =
    renderAsync:
      env.security.forms.reopen.map: baseForm =>
        html.account.reopen.form(form.foldLeft(baseForm)(_ withForm _), msg)

  def reopen = Open:
    auth.RedirectToProfileIfLoggedIn:
      Ok async renderReopen(none, none)

  def reopenApply = OpenBody:
    env.security.hcaptcha.verify() flatMap { captcha =>
      if captcha.ok then
        env.security.forms.reopen flatMap {
          _.form
            .bindFromRequest()
            .fold(
              err => renderReopen(err.some, none) map { BadRequest(_) },
              data =>
                env.security.reopen
                  .prepare(data.username, data.email, env.mod.logApi.closedByMod) flatMap {
                  case Left((code, msg)) =>
                    lila.mon.user.auth.reopenRequest(code).increment()
                    renderReopen(none, msg.some) map { BadRequest(_) }
                  case Right(user) =>
                    auth.MagicLinkRateLimit(user, data.email, ctx.req, rateLimitedFu):
                      lila.mon.user.auth.reopenRequest("success").increment()
                      env.security.reopen.send(user, data.email) inject Redirect:
                        routes.Account.reopenSent
                }
            )
        }
      else renderReopen(none, none) map { BadRequest(_) }
    }

  def reopenSent = Open:
    Ok.page(html.account.reopen.sent)

  def reopenLogin(token: String) = Open:
    env.security.reopen confirm token flatMap {
      case None =>
        lila.mon.user.auth.reopenConfirm("token_fail").increment()
        notFound
      case Some(user) =>
        env.report.api.reopenReports(lila.report.Suspect(user)) >>
          auth.authenticateUser(user, remember = true) >>-
          lila.mon.user.auth.reopenConfirm("success").increment().unit
    }

  def data = Auth { _ ?=> me ?=>
    val userId: UserId = getUserStr("user")
      .map(_.id)
      .filter(id => me.is(id) || isGranted(_.Impersonate)) | me.userId
    env.user.repo byId userId flatMapz { user =>
      if getBool("text") then
        apiC.GlobalConcurrencyLimitUser(me)(env.api.personalDataExport(user)): source =>
          Ok.chunked(source.map(_ + "\n"))
            .pipe(asAttachmentStream(s"lichess_${user.username}.txt"))
      else Ok.page(html.account.bits.data(user))
    }
  }
