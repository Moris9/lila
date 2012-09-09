package controllers

import lila._
import views._
import tournament.{ Created }
import http.Context

import scalaz.effects._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._

object Tournament extends LilaController {

  private def repo = env.tournament.repo
  private def forms = env.tournament.forms
  private def api = env.tournament.api
  private def socket = env.tournament.socket

  val home = Open { implicit ctx ⇒
    IOk(repo.created map { tournaments ⇒
      html.tournament.home(tournaments)
    })
  }

  def show(id: String) = Open { implicit ctx ⇒
    IOptionOk(repo byId id) {
      case t: Created ⇒ html.tournament.show.created(t)
      case _          ⇒ throw new Exception("oups")
    }
  }

  def form = Auth { implicit ctx ⇒
    me ⇒
      Ok(html.tournament.form(forms.create))
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒
      IOResult {
        implicit val req = ctx.body
        forms.create.bindFromRequest.fold(
          err ⇒ io(BadRequest(html.message.form(err))),
          setup ⇒ api.createTournament(setup, me).map(tournament ⇒
            Redirect(routes.Tournament.show(tournament.id))
          ))
      }
  }

  def websocket(id: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.join(id, getInt("version"), get("uid"), ctx.me).unsafePerformIO
  }
}
