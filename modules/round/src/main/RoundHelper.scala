package lila.round

import scala.math.{ min, max, round }

import play.api.libs.json.Json

import lila.game.Pov
import lila.pref.PrefHelper
import lila.round.Env.{ current ⇒ roundEnv }
import lila.user.Context

trait RoundHelper { self: PrefHelper ⇒

  def moretimeSeconds = roundEnv.moretimeSeconds

  def gameAnimationDelay = roundEnv.animationDelay

  def roundPlayerJsData(pov: Pov, version: Int)(implicit ctx: Context) = {
    import pov._
    val pref = userPref
    Json.obj(
      "game" -> Json.obj(
        "id" -> gameId,
        "started" -> game.started,
        "finished" -> game.finishedOrAborted,
        "clock" -> game.hasClock,
        "clockRunning" -> game.isClockRunning,
        "player" -> game.turnColor.name,
        "turns" -> game.turns,
        "lastMove" -> game.castleLastMoveTime.lastMoveString),
      "player" -> Json.obj(
        "id" -> player.id,
        "color" -> player.color.name,
        "version" -> version,
        "spectator" -> false
      ),
      "opponent" -> Json.obj(
        "color" -> opponent.color.name,
        "ai" -> opponent.isAi
      ),
      "possible_moves" -> possibleMoves(pov),
      "animation_delay" -> animationDelay(pov),
      "autoQueen" -> pref.autoQueen,
      "clockTenths" -> pref.clockTenths,
      "enablePremove" -> pref.premove,
      "tournament_id" -> game.tournamentId
    )
  }

  def roundWatcherJsData(pov: Pov, version: Int, tv: Boolean)(implicit ctx: Context) = {
    import pov._
    val pref = userPref
    Json.obj(
      "game" -> Json.obj(
        "id" -> gameId,
        "started" -> game.started,
        "finished" -> game.finishedOrAborted,
        "clock" -> game.hasClock,
        "clockRunning" -> game.isClockRunning,
        "player" -> game.turnColor.name,
        "turns" -> game.turns,
        "lastMove" -> game.castleLastMoveTime.lastMoveString),
      "player" -> Json.obj(
        "color" -> player.color.name,
        "version" -> version,
        "spectator" -> true),
      "opponent" -> Json.obj(
        "color" -> opponent.color.name,
        "ai" -> opponent.isAi),
      "possible_moves" -> possibleMoves(pov),
      "animation_delay" -> animationDelay(pov),
      "clockTenths" -> pref.clockTenths,
      "tv" -> tv
    )
  }

  private def possibleMoves(pov: Pov) = (pov.game playableBy pov.player) option {
    pov.game.toChess.situation.destinations map {
      case (from, dests) ⇒ from.key -> (dests.mkString)
    } toMap
  }

  private def animationDelay(pov: Pov) = round {
    roundEnv.animationDelay.toMillis *
      max(0, min(1.2,
        ((pov.game.estimateTotalTime - 60) / 60) * 0.2
      ))
  }
}
