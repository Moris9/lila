package lila.game

import chess.format.{ pgn ⇒ chessPgn }
import chess.Status

object Rewind {

  def apply(
    game: Game,
    pgn: String,
    initialFen: Option[String]): Valid[(Progress, String)] = chessPgn.Reader.withSans(
    pgn = pgn,
    op = sans ⇒ sans.isEmpty.fold(sans, sans.init),
    tags = initialFen.??(fen ⇒ List(
      chessPgn.Tag(_.FEN, fen),
      chessPgn.Tag(_.Variant, game.variant.name)
    ))
  ) map { replay ⇒
      val rewindedGame = replay.game
      val rewindedHistory = rewindedGame.board.history
      val rewindedSituation = rewindedGame.situation
      def rewindPlayer(player: Player) = player.copy(
        ps = player encodePieces rewindedGame.allPieces,
        isProposingTakeback = false)
      Progress(game, game.copy(
        whitePlayer = rewindPlayer(game.whitePlayer),
        blackPlayer = rewindPlayer(game.blackPlayer),
        turns = rewindedGame.turns,
        positionHashes = rewindedHistory.positionHashes mkString,
        castles = rewindedHistory.castleNotation,
        lastMove = rewindedHistory.lastMove map { case (a, b) ⇒ a + " " + b },
        status =
          if (rewindedSituation.checkMate) Status.Mate
          else if (rewindedSituation.staleMate) Status.Stalemate
          else if (rewindedSituation.autoDraw) Status.Draw
          else game.status,
        clock = game.clock map (_.switch),
        check = if (rewindedSituation.check) rewindedSituation.kingPos else None,
        lastMoveTime = nowSeconds.some
      )) -> rewindedGame.pgnMoves
    }
}
