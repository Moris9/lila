package lila.ai

import chess.format.{ UciMove, UciDump }
import chess.Move

import lila.analyse.AnalysisMaker
import lila.game.{ Game, Progress, GameRepo, PgnRepo, UciMemo }

trait Ai {

  def play(game: Game, level: Int): Fu[Progress] = (withValidSituation(game) {
    for {
      fen ← (game.variant.exotic ?? { GameRepo initialFen game.id }).thenPp
      pgn ← (PgnRepo get game.id).thenPp
      uciMoves ← (uciMemo.get(game, pgn) map (_ mkString " ")).thenPp
      moveStr ← move(uciMoves, fen, level).thenPp
      uciMove ← (UciMove(moveStr) toValid "Wrong bestmove: " + moveStr).future.thenPp
      result ← (game.toChess withPgnMoves pgn)(uciMove.orig, uciMove.dest).future.thenPp
      (c, m) = result
      (progress, pgn2) = game.update(c, m)
      _ ← (GameRepo save progress) >>
        PgnRepo.save(game.id, pgn2) >>-
        uciMemo.add(game, uciMove.uci)
    } yield progress
  }).thenPp

  def uciMemo: UciMemo

  def move(uciMoves: String, initialFen: Option[String], level: Int): Fu[String]

  def analyse(uciMoves: String, initialFen: Option[String]): Fu[AnalysisMaker]

  private def withValidSituation[A](game: Game)(op: ⇒ Fu[A]): Fu[A] =
    if (game.toChess.situation playable true) op
    else fufail("[ai stockfish] invalid game situation: " + game.toChess.situation)
}
