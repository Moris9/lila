package lila.app
package mashup

import lila.api.Context
import lila.common.LightUser
import lila.event.Event
import lila.forum.MiniForumPost
import lila.game.{ Game, Pov, GameRepo }
import lila.playban.TempBan
import lila.simul.Simul
import lila.timeline.Entry
import lila.tournament.{ Tournament, Winner }
import lila.tv.{ Tv, StreamOnAir }
import lila.user.User
import play.api.libs.json._

final class Preload(
    tv: Tv,
    leaderboard: Boolean => Fu[List[User.LightPerf]],
    tourneyWinners: Fu[List[Winner]],
    timelineEntries: String => Fu[List[Entry]],
    streamsOnAir: () => Fu[List[StreamOnAir]],
    dailyPuzzle: () => Fu[Option[lila.puzzle.DailyPuzzle]],
    countRounds: () => Int,
    lobbyApi: lila.api.LobbyApi,
    getPlayban: String => Fu[Option[TempBan]],
    lightUser: String => Option[LightUser]) {

  private type Response = (JsObject, List[Entry], List[MiniForumPost], List[Tournament], List[Event], List[Simul], Option[Game], List[User.LightPerf], List[Winner], Option[lila.puzzle.DailyPuzzle], List[StreamOnAir], List[lila.blog.MiniPost], Option[TempBan], Option[Preload.CurrentGame], Int)

  def apply(
    posts: Fu[List[MiniForumPost]],
    tours: Fu[List[Tournament]],
    events: Fu[List[Event]],
    simuls: Fu[List[Simul]])(implicit ctx: Context): Fu[Response] =
    lobbyApi(ctx) zip
      posts zip
      tours zip
      events zip
      simuls zip
      tv.getBestGame zip
      (ctx.userId ?? timelineEntries) zip
      leaderboard(true) zip
      tourneyWinners zip
      dailyPuzzle() zip
      streamsOnAir() zip
      (ctx.userId ?? getPlayban) zip
      (ctx.me ?? Preload.currentGame(lightUser)) map {
        case (((((((((((((data, posts), tours), events), simuls), feat), entries), lead), tWinners), puzzle), streams), playban), currentGame)) =>
          (data, entries, posts, tours, events, simuls, feat, lead, tWinners, puzzle, streams, Env.blog.lastPostCache.apply, playban, currentGame, countRounds())
      }
}

object Preload {

  case class CurrentGame(pov: Pov, json: JsObject, opponent: String)

  def currentGame(lightUser: String => Option[LightUser])(user: User) =
    GameRepo.urgentGames(user) map { povs =>
      povs.find { p =>
        p.game.nonAi && p.game.hasClock && p.isMyTurn
      } map { pov =>
        val opponent = lila.game.Namer.playerString(pov.opponent)(lightUser)
        CurrentGame(
          pov = pov,
          opponent = opponent,
          json = Json.obj(
            "id" -> pov.game.id,
            "color" -> pov.color.name,
            "opponent" -> opponent))
      }
    }
}
