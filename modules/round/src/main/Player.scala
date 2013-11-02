package lila.round

import chess.format.Forsyth
import chess.Pos.posAt
import chess.{ Status, Role, Color }

import actorApi.round.{ HumanPlay, AiPlay, DrawNo, TakebackNo, PlayResult, Cheat }
import lila.ai.Ai
import lila.game.{ Game, GameRepo, PgnRepo, Pov, Progress, UciMemo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.MoveEvent

private[round] final class Player(
    engine: Ai,
    bus: lila.common.Bus,
    finisher: Finisher,
    cheatDetector: CheatDetector,
    roundMap: akka.actor.ActorSelection,
    uciMemo: UciMemo,
    aiIp: String) {

  def human(play: HumanPlay)(pov: Pov): Fu[Events] = play match {
    case HumanPlay(playerId, ip, origS, destS, promS, blur, lag, onFailure) ⇒ pov match {
      case Pov(game, color) if (game playableBy color) ⇒
        PgnRepo get game.id flatMap { pgnString ⇒
          (for {
            orig ← posAt(origS) toValid "Wrong orig " + origS
            dest ← posAt(destS) toValid "Wrong dest " + destS
            promotion = Role promotable promS
            chessGame = game.toChess withPgnMoves pgnString
            newChessGameAndMove ← chessGame(orig, dest, promotion, lag)
            (newChessGame, move) = newChessGameAndMove
          } yield game.update(newChessGame, move, blur) -> move).prefixFailuresWith(playerId + " - ").future flatMap {
            case ((progress, pgn), move) ⇒
              ((GameRepo save progress) zip PgnRepo.save(pov.gameId, pgn)) >>-
                (pov.game.hasAi ! uciMemo.add(pov.game, move)) >>-
                notifyProgress(move, progress, ip) >>
                progress.game.finished.fold(
                  moveFinish(progress.game, color) map { progress.events ::: _ }, {
                    cheatDetector(progress.game) addEffect {
                      case Some(color) ⇒ roundMap ! Tell(game.id, Cheat(color))
                      case None ⇒ {
                        if (progress.game.playableByAi) roundMap ! Tell(game.id, AiPlay)
                        if (game.player.isOfferingDraw) roundMap ! Tell(game.id, DrawNo(game.player.id))
                        if (game.player.isProposingTakeback) roundMap ! Tell(game.id, TakebackNo(game.player.id))
                      }
                    } inject progress.events
                  })
          }
        } addFailureEffect onFailure
      case Pov(game, _) if game.finished           ⇒ fufail(s"$pov game is finished")
      case Pov(game, _) if game.aborted            ⇒ fufail(s"$pov game is aborted")
      case Pov(game, color) if !game.turnOf(color) ⇒ fufail(s"$pov not your turn")
      case _                                       ⇒ fufail(s"$pov move refused for some reason")
    }
  }

  def ai(game: Game): Fu[Events] =
    (game.playable && game.player.isAi).fold(
      engine.play(game, game.aiLevel | 1) flatMap {
        case (progress, move) ⇒ {
          notifyProgress(move, progress, aiIp)
          moveFinish(progress.game, game.turnColor) map { progress.events ::: _ }
        }
      },
      fufail("not AI turn")
    ) logFailureErr "[ai play] game %s turn %d".format(game.id, game.turns)

  private def notifyProgress(move: chess.Move, progress: Progress, ip: String) {
    val game = progress.game
    val chess = game.toChess
    val meta = {
      if (move.captures) "x" else ""
    } + {
      if (game.finished) "#" else if (chess.situation.check) "+" else ""
    }
    bus.publish(MoveEvent(
      ip = ip,
      gameId = game.id,
      fen = Forsyth exportBoard chess.board,
      move = move.keyString,
      meta = meta), 'moveEvent)
  }

  private def moveFinish(game: Game, color: Color): Fu[Events] = game.status match {
    case Status.Mate                             ⇒ finisher(game, _.Mate, Some(color))
    case status@(Status.Stalemate | Status.Draw) ⇒ finisher(game, _ ⇒ status)
    case _                                       ⇒ fuccess(Nil)
  }
}
