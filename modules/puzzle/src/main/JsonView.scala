package lila.puzzle

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.puzzle._
import lila.tree

final class JsonView(
    gameJson: GameJson,
    animationDuration: scala.concurrent.duration.Duration) {

  def apply(
    puzzle: Puzzle,
    userInfos: Option[UserInfos],
    mode: String,
    isMobileApi: Boolean,
    round: Option[Round] = None,
    result: Option[Result] = None,
    voted: Option[Boolean]): Fu[JsObject] =
    (!isMobileApi ?? gameJson(puzzle.gameId, puzzle.initialPly).map(_.some)) map { gameJson =>
      Json.obj(
        "game" -> gameJson,
        "puzzle" -> Json.obj(
          "id" -> puzzle.id,
          "rating" -> puzzle.perf.intRating,
          "attempts" -> puzzle.attempts,
          "fen" -> puzzle.fen,
          "color" -> puzzle.color.name,
          "initialMove" -> isMobileApi.option(puzzle.initialMove.uci),
          "initialPly" -> puzzle.initialPly,
          "gameId" -> puzzle.gameId,
          "lines" -> lila.puzzle.Line.toJson(puzzle.lines),
          "branch" -> (!isMobileApi).option(makeBranch(puzzle)),
          "enabled" -> puzzle.enabled,
          "vote" -> puzzle.vote.sum
        ).noNull,
        "mode" -> mode,
        "attempt" -> round.ifTrue(isMobileApi).map { r =>
          Json.obj(
            "userRatingDiff" -> r.ratingDiff,
            "win" -> r.result.win,
            "seconds" -> "a few" // lol we don't have the value anymore
          )
        },
        "voted" -> voted,
        "user" -> userInfos.map(JsonView.infos(isMobileApi)),
        "difficulty" -> isMobileApi.option {
          Json.obj(
            "choices" -> Json.arr(
              Json.arr(2, "Normal")
            ),
            "current" -> 2
          )
        }).noNull
    }

  def pref(p: lila.pref.Pref) = Json.obj(
    "coords" -> p.coords,
    "rookCastle" -> p.rookCastle,
    "animation" -> Json.obj(
      "duration" -> p.animationFactor * animationDuration.toMillis
    ),
    "moveEvent" -> p.moveEvent,
    "highlight" -> p.highlight)

  private def makeBranch(puzzle: Puzzle): Option[tree.Branch] = {
    import chess.format._
    val fullSolution: List[Uci.Move] = (Line solution puzzle.lines).map { uci =>
      Uci.Move(uci) err s"Invalid puzzle solution UCI $uci"
    }
    val solution =
      if (fullSolution.isEmpty) {
        logger.warn(s"Puzzle ${puzzle.id} has an empty solution from ${puzzle.lines}")
        fullSolution
      }
      else if (fullSolution.size % 2 == 0) fullSolution.init
      else fullSolution
    val init = chess.Game(none, puzzle.fenAfterInitialMove).withTurns(puzzle.initialPly)
    val (_, branchList) = solution.foldLeft[(chess.Game, List[tree.Branch])]((init, Nil)) {
      case ((prev, branches), uci) =>
        val (game, move) = prev(uci.orig, uci.dest, uci.promotion).prefixFailuresWith(s"puzzle ${puzzle.id}").err
        val branch = tree.Branch(
          id = UciCharPair(move.toUci),
          ply = game.turns,
          move = Uci.WithSan(move.toUci, game.pgnMoves.last),
          fen = chess.format.Forsyth >> game,
          check = game.situation.check,
          crazyData = none)
        (game, branch :: branches)
    }
    branchList.foldLeft[Option[tree.Branch]](None) {
      case (None, branch)        => branch.some
      case (Some(child), branch) => Some(branch addChild child)
    }
  }
}

object JsonView {

  def infos(isMobileApi: Boolean)(i: UserInfos): JsObject = Json.obj(
    "rating" -> i.user.perfs.puzzle.intRating,
    "history" -> isMobileApi.option(i.history.map(_.rating)), // for mobile BC
    "recent" -> i.history.map { r =>
      Json.arr(r.puzzleId, r.ratingDiff, r.rating)
    }
  ).noNull

  def round(r: Round): JsObject = Json.obj(
    "ratingDiff" -> r.ratingDiff,
    "win" -> r.result.win
  )
}
