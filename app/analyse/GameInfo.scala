package lila
package analyse

import chess.OpeningExplorer
import chess.format.Forsyth
import game.DbGame

import scalaz.effects.IO

final class GameInfo private (
    val game: DbGame,
    val pgn: String,
    val opening: Option[OpeningExplorer.Opening]) {

  def toMap = Map(
    "pgn" -> pgn,
    "opening" -> game.variant.standard.fold(
      opening map { o ⇒ Map("code" -> o.code, "name" -> o.name) },
      None)
  )
}

object GameInfo {

  def apply(pgnDump: PgnDump)(game: DbGame): IO[GameInfo] =
    pgnDump >> game map { pgn ⇒
      new GameInfo(
        game = game,
        pgn = pgn,
        opening = OpeningExplorer openingOf game.pgn)
    }
}
