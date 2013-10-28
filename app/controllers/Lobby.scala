package controllers

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.mvc._

import lila.app._
import lila.common.LilaCookie
import lila.tournament.TournamentRepo
import lila.user.Context
import views._

object Lobby extends LilaController {

  def home = Open { implicit ctx ⇒
    renderHome(Results.Ok).map(_.withHeaders(
      CACHE_CONTROL -> "no-cache", PRAGMA -> "no-cache"
    ))
  }

  def handleStatus(req: RequestHeader, status: Results.Status): Fu[SimpleResult] =
    reqToCtx(req) flatMap { ctx ⇒ renderHome(status)(ctx) }

  def renderHome[A](status: Results.Status)(implicit ctx: Context): Fu[SimpleResult] =
    Env.current.preloader(
      posts = Env.forum.recent(ctx.me, Env.team.cached.teamIds.apply),
      tours = TournamentRepo.createdUnprotected,
      filter = Env.setup.filter
    ).map(_.fold(Redirect(_), {
        case (preload, entries, gameEntries, posts, tours, featured) ⇒ status(html.lobby.home(
          Json stringify preload, entries, gameEntries, posts, tours, featured)) |> { response ⇒
          ctx.req.session.data.contains(LilaCookie.sessionId).fold(
            response,
            response withCookies LilaCookie.makeSessionId(ctx.req)
          )
        }
      }))

  def socket = Socket[JsValue] { implicit ctx ⇒
    get("sri") ?? { uid ⇒
      Env.lobby.socketHandler(uid = uid, user = ctx.me)
    }
  }

  def timeline = Auth { implicit ctx ⇒
    me ⇒
      Env.timeline.getter.userEntries(me.id) map { html.timeline.entries(_) }
  }

  def timelineMore = Auth { implicit ctx ⇒
    me ⇒
      Env.timeline.getter.moreUserEntries(me.id) map { html.timeline.more(_) }
  }
}
