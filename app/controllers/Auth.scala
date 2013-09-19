package controllers

import lila.app._
import lila.common.LilaCookie
import lila.user.{ UserRepo, HistoryRepo }
import views._

import play.api.mvc._, Results._
import play.api.data._, Forms._

object Auth extends LilaController {

  private def api = Env.security.api
  private def forms = Env.security.forms

  private def gotoLoginSucceeded[A](username: String)(implicit req: RequestHeader) =
    api saveAuthentication username map { sessionId ⇒
      val uri = req.session.get(api.AccessUri) | routes.Lobby.home.url
      Redirect(uri) withCookies LilaCookie.withSession { session ⇒
        session + ("sessionId" -> sessionId) - api.AccessUri
      }
    }

  private def gotoSignupSucceeded[A](username: String)(implicit req: RequestHeader) =
    api saveAuthentication username map { sessionId ⇒
      Redirect(routes.User.show(username)) withCookies LilaCookie.session("sessionId", sessionId)
    }

  def login = Open { implicit ctx ⇒
    Ok(html.auth.login(api.loginForm)) fuccess
  }

  def authenticate = OpenBody { implicit ctx ⇒
    Firewall {
      implicit val req = ctx.body
      api.loginForm.bindFromRequest.fold(
        err ⇒ BadRequest(html.auth.login(err)).fuccess,
        _.fold(InternalServerError("authenticate error").fuccess) { user ⇒
          user.ipBan.fold(
            Env.security.firewall.blockIp(req.remoteAddress) inject BadRequest("blocked by firewall"),
            gotoLoginSucceeded(user.username)
          )
        }
      )
    }
  }

  def logout = Open { implicit ctx ⇒
    gotoLogoutSucceeded(ctx.req) fuccess
  }

  def signup = Open { implicit ctx ⇒
    forms.signupWithCaptcha map {
      case (form, captcha) ⇒ Ok(html.auth.signup(form, captcha))
    }
  }

  def signupPost = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    forms.signup.bindFromRequest.fold(
      err ⇒ forms.anyCaptcha map { captcha ⇒
        BadRequest(html.auth.signup(err, captcha))
      },
      data ⇒ Firewall {
        UserRepo.create(data.username, data.password) flatMap { userOption ⇒
          val user = userOption err "No user could be created for %s".format(data.username)
          HistoryRepo.addEntry(user.id, user.elo, none) >>
            gotoSignupSucceeded(user.username)
        }
      }
    )
  }

  private def gotoLogoutSucceeded(implicit req: RequestHeader) = {
    req.session get "sessionId" foreach lila.security.Store.delete
    logoutSucceeded(req) withCookies LilaCookie.newSession
  }

  private def logoutSucceeded(req: RequestHeader): SimpleResult =
    Redirect(routes.Lobby.home)
}
