package lila.ai

import chess.format.{ UciMove, UciDump }
import chess.Move

import lila.analyse.Info
import lila.game.{ Game, Progress, GameRepo, PgnRepo, UciMemo }

trait Ai {

  def play(game: Game, level: Int): Fu[Progress] = withValidSituation(game) {
    for {
      fen ← game.variant.exotic ?? { GameRepo initialFen game.id }
      pgn ← PgnRepo get game.id
      uciMoves ← uciMemo.get(game, pgn)
      moveStr ← move(uciMoves.toList, fen, level)
      uciMove ← (UciMove(moveStr) toValid s"${game.id} wrong bestmove: $moveStr").future
      result ← (game.toChess withPgnMoves pgn)(uciMove.orig, uciMove.dest).future
      (c, m) = result
      (progress, pgn2) = game.update(c, m)
      _ ← (GameRepo save progress) >>
        PgnRepo.save(game.id, pgn2) >>-
        uciMemo.add(game, uciMove.uci)
    } yield progress
  }

  def uciMemo: UciMemo

  def move(uciMoves: List[String], initialFen: Option[String], level: Int): Fu[String]

  def analyse(uciMoves: List[String], initialFen: Option[String]): Fu[List[Info]]

  private def withValidSituation[A](game: Game)(op: ⇒ Fu[A]): Fu[A] =
    if (game.toChess.situation playable true) op
    else fufail("[ai stockfish] invalid game situation: " + game.toChess.situation)
}
