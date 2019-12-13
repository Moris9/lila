package lila.puzzle

import chess.format.{ Forsyth, Uci, UciCharPair }
import chess.opening.FullOpeningDB
import lila.game.Game
import lila.tree

object TreeBuilder {

  def apply(game: Game, plies: Int): tree.Root = {
    chess.Replay.gameMoveWhileValid(game.pgnMoves take plies, Forsyth.initial, game.variant) match {
      case (init, games, error) =>
        error foreach logChessError(game.id)
        val fen = Forsyth >> init
        val root = tree.Root(
          ply = init.turns,
          fen = fen,
          check = init.situation.check,
          opening = FullOpeningDB findByFen fen,
          crazyData = None
        )
        def makeBranch(g: chess.Game, m: Uci.WithSan) = {
          val fen = Forsyth >> g
          tree.Branch(
            id = UciCharPair(m.uci),
            ply = g.turns,
            move = m,
            fen = fen,
            check = g.situation.check,
            opening = FullOpeningDB findByFen fen,
            crazyData = None
          )
        }
        games.reverse match {
          case Nil => root
          case (g, m) :: rest =>
            root prependChild rest.foldLeft(makeBranch(g, m)) {
              case (node, (g, m)) => makeBranch(g, m) prependChild node
            }
        }
    }
  }

  private val logChessError = (id: Game.ID) =>
    (err: String) =>
      logger.warn(s"TreeBuilder https://lichess.org/$id ${err.linesIterator.toList.headOption}")
}
