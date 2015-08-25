package controllers

import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.game.{ GameRepo, Game => GameModel, Pov }
import views._

object Tv extends LilaController {

  def index = onChannel(lila.tv.Tv.Channel.Best.key)

  def onChannel(chanKey: String) = Open { implicit ctx =>
    (lila.tv.Tv.Channel.byKey get chanKey).fold(notFound)(lichessTv)
  }

  def sides(chanKey: String, gameId: String, color: String) = Open { implicit ctx =>
    lila.tv.Tv.Channel.byKey get chanKey match {
      case None => notFound
      case Some(channel) =>
        OptionFuResult(GameRepo.pov(gameId, color)) { pov =>
          Env.tv.tv.getChampions zip
            Env.game.crosstableApi(pov.game) map {
              case (champions, crosstable) => Ok(html.tv.sides(channel, champions, pov, crosstable, streams = Nil))
            }
        }
    }
  }

  private def lichessTv(channel: lila.tv.Tv.Channel)(implicit ctx: Context) =
    OptionFuResult(Env.tv.tv getGame channel) { game =>
      val flip = getBool("flip")
      val pov = flip.fold(Pov second game, Pov first game)
      val onTv = lila.round.OnTv(channel.key, flip)
      negotiate(
        html = {
          Env.api.roundApi.watcher(pov, lila.api.Mobile.Api.currentVersion, tv = onTv.some) zip
            Env.game.crosstableApi(game) zip
            Env.tv.tv.getChampions map {
              case ((data, cross), champions) =>
                Ok(html.tv.index(channel, champions, pov, data, cross, flip))
            }
        },
        api = apiVersion => Env.api.roundApi.watcher(pov, apiVersion, tv = onTv.some) map { Ok(_) }
      )
    }

  def streamIn(id: String) = Open { implicit ctx =>
    Env.tv.streamsOnAir flatMap { streams =>
      streams find (_.id == id) match {
        case None    => notFound
        case Some(s) => fuccess(Ok(html.tv.stream(s, streams filterNot (_.id == id))))
      }
    }
  }

  def streamOut = Action.async {
    import makeTimeout.short
    import akka.pattern.ask
    import lila.round.TvBroadcast
    import play.api.libs.EventSource
    implicit val encoder = play.api.libs.Comet.CometMessage.jsonMessages
    Env.round.tvBroadcast ? TvBroadcast.GetEnumerator mapTo
      manifest[TvBroadcast.EnumeratorType] map { enum =>
        Ok.chunked(enum &> EventSource()).as("text/event-stream")
      }
  }

  def embed = Action { req =>
    Ok {
      val bg = get("bg", req) | "light"
      val theme = get("theme", req) | "brown"
      val url = s"""${req.domain + routes.Tv.frame}?bg=$bg&theme=$theme"""
      s"""document.write("<iframe src='http://$url&embed=" + document.domain + "' class='lichess-tv-iframe' allowtransparency='true' frameBorder='0' style='width: 224px; height: 264px;' title='Lichess free online chess'></iframe>");"""
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def frame = Action.async { req =>
    Env.tv.tv.getBest map {
      case None => NotFound
      case Some(game) => Ok(views.html.tv.embed(
        Pov first game,
        get("bg", req) | "light",
        lila.pref.Theme(~get("theme", req)).cssClass
      ))
    }
  }
}
