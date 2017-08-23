package controllers

import play.api.libs.json._

import lila.app._
import lila.common.HTTPRequest
import views._

object Timeline extends LilaController {

  def home = Auth { implicit ctx =>
    import lila.timeline.Entry.entryWrites
    val nb = getInt("nb").fold(100)(_ min 100)
    me =>
      negotiate(
        html = {
          if (HTTPRequest.isXhr(ctx.req))
            Env.timeline.entryApi.userEntries(me.id) map { html.timeline.entries(_) }
          else {
            Env.timeline.entryApi.moreUserEntries(me.id, nb) map { html.timeline.more(_) }
          }
        },
        _ => {
          val entries = Env.timeline.entryApi.moreUserEntries(me.id, nb)
          entries map { es => Ok(Json.obj("entries" -> es)) }
        }
      )
  }

  def unsub(channel: String) = Auth { implicit ctx => me =>
    Env.timeline.unsubApi.set(channel, me.id, ~get("unsub") == "on")
  }
}
