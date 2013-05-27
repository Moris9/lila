package controllers

import lila.app._
import views._
import lila.user.{ User ⇒ UserModel, UserRepo, Context }

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Message extends LilaController {

  private def api = Env.message.api
  private def forms = Env.message.forms

  def inbox(page: Int) = Auth { implicit ctx ⇒
    implicit me ⇒ api.inbox(me, page) map { html.message.inbox(_) }
  }

  def thread(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      OptionOk(api.thread(id, me)) { html.message.thread(_, forms.post) }
  }

  def answer(id: String) = AuthBody { implicit ctx ⇒
    implicit me ⇒
      OptionFuResult(api.thread(id, me)) { thread ⇒
        implicit val req = ctx.body
        forms.post.bindFromRequest.fold(
          err ⇒ BadRequest(html.message.thread(thread, err)).fuccess,
          text ⇒ api.makePost(thread, text, me) inject Redirect(routes.Message.thread(thread.id) + "#bottom")
        )
      }
  }

  def form = Auth { implicit ctx ⇒
    implicit me ⇒
      get("username") ?? UserRepo.named map { user ⇒
        Ok(html.message.form(forms.thread, user))
      }
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒
      implicit val req = ctx.body
      forms.thread.bindFromRequest.fold(
        err ⇒ BadRequest(html.message.form(err)).fuccess,
        data ⇒ api.makeThread(data, me) map { thread ⇒
          Redirect(routes.Message.thread(thread.id))
        })
  }

  def delete(id: String) = AuthBody { implicit ctx ⇒
    implicit me ⇒
      api.deleteThread(id, me) inject Redirect(routes.Message.inbox(1))
  }
}
