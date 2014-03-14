package controllers

import play.api.mvc._
import play.api.templates.Html

import lila.app._
import lila.game.{ GameRepo, Game => GameModel, Pov }
import lila.tournament.TournamentRepo
import lila.user.{ UserRepo, Confrontation }
import views._

object Tv extends LilaController {

  def index = Open { implicit ctx =>
    OptionFuResult(Env.game.featured.one) { game =>
      Env.round.version(game.id) zip
        (GameRepo onTv 10) zip
        confrontation(game) zip
        (game.tournamentId ?? TournamentRepo.byId) map {
          case (((v, games), confrontation), tour) =>
            val flip = getBool("flip")
            Ok(html.tv.index(
              flip.fold(Pov second game, Pov first game),
              v, games, confrontation, tour, flip))
        }
    }
  }

  def embed = Action { req =>
    Ok {
      val bg = get("bg", req) | "light"
      val theme = get("theme", req) | "brown"
      val url = s"""${req.domain + routes.Tv.frame}?bg=$bg&theme=$theme"""
      s"""document.write("<iframe src='http://$url&embed=" + document.domain + "' class='lichess-tv-iframe' allowtransparency='true' frameBorder='0' style='width: 226px; height: 266px;' title='Lichess free online chess'></iframe>");"""
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def frame = Action.async { req =>
    Env.game.featured.one map {
      case None => NotFound
      case Some(game) => Ok(views.html.tv.embed(
        game,
        get("bg", req) | "light",
        lila.pref.Theme(~get("theme", req)).cssClass
      ))
    }
  }

  def stream = Action.async {
    import makeTimeout.short
    import akka.pattern.ask
    import lila.round.TvBroadcast
    import play.api.libs.EventSource
    import play.api.libs.Comet.CometMessage
    implicit val encoder = CometMessage.jsonMessages
    Env.round.tvBroadcast ? TvBroadcast.GetEnumerator mapTo
      manifest[TvBroadcast.EnumeratorType] map { enum =>
        Ok.chunked(enum &> EventSource()).as("text/event-stream")
      }
  }

  private def confrontation(game: GameModel): Fu[Option[Confrontation]] = ~{
    (game.firstPlayer.userId |@| game.secondPlayer.userId) apply {
      case (id1, id2) => (UserRepo byId id1) zip (UserRepo byId id2) flatMap {
        case (Some(user1), Some(user2)) => Env.game.cached.confrontation(user1, user2) map (_.some)
        case _                          => fuccess(none)
      }
    }
  }
}
