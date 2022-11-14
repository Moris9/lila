package controllers

import play.api.mvc.*
import play.api.libs.json.Json
import scalatags.Text.all.Frag

import lila.api.Context
import lila.app.{ *, given }
import lila.memo.CacheApi.*
import views.*

final class KeyPages(env: Env)(using ec: scala.concurrent.ExecutionContext):

  def home(status: Results.Status)(implicit ctx: Context): Fu[Result] =
    homeHtml
      .map { html =>
        env.lilaCookie.ensure(ctx.req)(status(html))
      }

  def homeHtml(implicit ctx: Context): Fu[Frag] =
    env
      .preloader(
        tours = env.tournament.cached.onHomepage.getUnit.recoverDefault,
        swiss = env.swiss.feature.onHomepage.getUnit.getIfPresent,
        events = env.event.api.promoteTo(ctx.req).recoverDefault,
        simuls = env.simul.allCreatedFeaturable.get {}.recoverDefault,
        streamerSpots = env.streamer.homepageMaxSetting.get()
      )
      .mon(_.lobby segment "preloader.total")
      .map { h =>
        lila.mon.chronoSync(_.lobby segment "renderSync") {
          html.lobby.home(h)
        }
      }

  def notFound(ctx: Context): Result =
    Results.NotFound(html.base.notFound()(ctx))

  def blacklisted(implicit ctx: Context): Result =
    if (lila.api.Mobile.Api requested ctx.req)
      Results.Unauthorized(
        Json.obj(
          "error" -> html.site.message.blacklistedMessage
        )
      )
    else Results.Unauthorized(html.site.message.blacklistedMessage)
