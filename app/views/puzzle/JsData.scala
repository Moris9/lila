package views.html.puzzle

import play.api.libs.json.{ JsArray, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.puzzle._

object JsData extends lila.Steroids {

  def apply(
    puzzle: Puzzle,
    userInfos: Option[UserInfos],
    mode: String,
    animationDuration: scala.concurrent.duration.Duration,
    round: Option[Round] = None,
    win: Option[Boolean] = None,
    voted: Option[Boolean])(implicit ctx: Context) = Json.obj(
    "puzzle" -> Json.obj(
      "id" -> puzzle.id,
      "rating" -> puzzle.perf.intRating,
      "attempts" -> puzzle.attempts,
      "fen" -> puzzle.fen,
      "color" -> puzzle.color.name,
      "initialMove" -> puzzle.initialMove,
      "initialPly" -> puzzle.initialPly,
      "gameId" -> puzzle.gameId,
      "lines" -> lila.puzzle.Line.toJson(puzzle.lines),
      "enabled" -> puzzle.enabled,
      "vote" -> puzzle.vote.sum
    ),
    "pref" -> Json.obj(
      "coords" -> ctx.pref.coords
    ),
    "chessground" -> Json.obj(
      "highlight" -> Json.obj(
        "lastMove" -> ctx.pref.highlight,
        "check" -> ctx.pref.highlight
      ),
      "movable" -> Json.obj(
        "showDests" -> ctx.pref.destination
      ),
      "draggable" -> Json.obj(
        "showGhost" -> ctx.pref.highlight
      ),
      "premovable" -> Json.obj(
        "showDests" -> ctx.pref.destination
      )
    ),
    "animation" -> Json.obj(
      "duration" -> ctx.pref.animationFactor * animationDuration.toMillis
    ),
    "mode" -> mode,
    "round" -> round.map { a =>
      Json.obj(
        "ratingDiff" -> a.ratingDiff,
        "win" -> a.win
      )
    },
    "win" -> win,
    "voted" -> voted,
    "user" -> userInfos.map { i =>
      Json.obj(
        "rating" -> i.user.perfs.puzzle.intRating,
        "history" -> i.history.map { r =>
          Json.arr(r.puzzleId, r.ratingDiff, r.rating)
        }
      )
    },
    "difficulty" -> ctx.isAuth.option {
      Json.obj(
        "choices" -> Json.arr(
          Json.arr(2, trans.difficultyNormal.str())
        ),
        "current" -> 2
      )
    })
}
