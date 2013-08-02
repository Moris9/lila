package controllers

import play.api.mvc._
import play.api.templates.Html
import views._

import lila.app._
import lila.game.Pov
import lila.round.WatcherRoomRepo
import lila.tournament.TournamentRepo

object Tv extends LilaController {

  def index = Open { implicit ctx ⇒
    OptionFuResult(Env.game.featured.one) { game ⇒
      Env.round.version(game.id) zip
        (WatcherRoomRepo room game.id map { room ⇒
          html.round.watcherRoomInner(room.decodedMessages)
        }) zip
        (game.tournamentId ?? TournamentRepo.byId) map {
          case ((v, roomHtml), tour) ⇒
            Ok(html.tv.index(
              Pov creator game,
              v,
              roomHtml,
              tour))
        }
    }
  }
}
