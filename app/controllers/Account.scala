package controllers

import play.api.mvc._, Results._

import lila.app._
import lila.common.LilaCookie
import lila.db.api.$find
import lila.security.Permission
import lila.user.tube.userTube
import lila.user.{ User => UserModel, UserRepo }
import views._

object Account extends LilaController {

  private def env = Env.user
  private def relationEnv = Env.relation
  private def forms = lila.user.DataForm

  def profile = Auth { implicit ctx =>
    me =>
      Ok(html.account.profile(me, forms profileOf me)).fuccess
  }

  def profileApply = AuthBody { implicit ctx =>
    me =>
      implicit val req: Request[_] = ctx.body
      FormFuResult(forms.profile) { err =>
        fuccess(html.account.profile(me, err))
      } { profile =>
        UserRepo.setProfile(me.id, profile) inject Redirect(routes.User show me.username)
      }
  }

  def info = Auth { implicit ctx =>
    me =>
      negotiate(
        html = notFound,
        api = _ => lila.game.GameRepo urgentGames me map { povs =>
          Env.current.bus.publish(lila.user.User.Active(me), 'userActive)
          Ok {
            import play.api.libs.json._
            Env.user.jsonView(me, extended = true) ++ Json.obj(
              "nowPlaying" -> JsArray(povs take 20 map Env.api.lobbyApi.nowPlaying))
          }
        }
      )
  }

  def passwd = Auth { implicit ctx =>
    me =>
      Ok(html.account.passwd(me, forms.passwd)).fuccess
  }

  def passwdApply = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      FormFuResult(forms.passwd) { err =>
        fuccess(html.account.passwd(me, err))
      } { data =>
        for {
          ok ← UserRepo.checkPassword(me.id, data.oldPasswd)
          _ ← ok ?? UserRepo.passwd(me.id, data.newPasswd1)
        } yield {
          val content = html.account.passwd(me, forms.passwd.fill(data), ok.some)
          ok.fold(Ok(content), BadRequest(content))
        }
      }
  }

  private def emailForm(id: String) = UserRepo email id map { email =>
    Env.security.forms.changeEmail.fill(
      lila.security.DataForm.ChangeEmail(~email, ""))
  }

  def email = Auth { implicit ctx =>
    me =>
      emailForm(me.id) map { form =>
        Ok(html.account.email(me, form))
      }
  }

  def emailApply = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      FormFuResult(Env.security.forms.changeEmail) { err =>
        fuccess(html.account.email(me, err))
      } { data =>
        val email = Env.security.emailAddress.validate(data.email) err s"Invalid email ${data.email}"
        for {
          ok ← UserRepo.checkPassword(me.id, data.passwd)
          _ ← ok ?? UserRepo.email(me.id, email)
          form <- emailForm(me.id)
        } yield {
          val content = html.account.email(me, form, ok.some)
          ok.fold(Ok(content), BadRequest(content))
        }
      }
  }

  def close = Auth { implicit ctx =>
    me =>
      Ok(html.account.close(me, Env.security.forms.closeAccount)).fuccess
  }

  def closeConfirm = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      FormFuResult(Env.security.forms.closeAccount) { err =>
        fuccess(html.account.close(me, err))
      } { password =>
        UserRepo.checkPassword(me.id, password) flatMap {
          case false => BadRequest(html.account.close(me, Env.security.forms.closeAccount)).fuccess
          case true =>
            (UserRepo disable me) >>
              relationEnv.api.unfollowAll(me.id) >>
              Env.team.api.quitAll(me.id) >>
              (Env.security disconnect me.id) inject {
                Redirect(routes.User show me.username) withCookies LilaCookie.newSession
              }
        }
      }
  }

  def kid = Auth { implicit ctx =>
    me =>
      Ok(html.account.kid(me)).fuccess
  }

  def kidConfirm = Auth { ctx =>
    me =>
      implicit val req = ctx.req
      (UserRepo toggleKid me) inject Redirect(routes.Account.kid)
  }
}
