package lila
package round

import game.{ GameRepo, DbGame, Pov }
import user.{ UserRepo, EloUpdater }
import i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import chess.{ EloCalculator, Status, Color }
import Status._
import Color._

import scalaz.effects._

final class Finisher(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    messenger: Messenger,
    eloUpdater: EloUpdater,
    eloCalculator: EloCalculator,
    finisherLock: FinisherLock) {

  type ValidIOEvents = Valid[IO[List[Event]]]

  def abort(pov: Pov): ValidIOEvents =
    if (pov.game.abortable) finish(pov.game, Aborted)
    else !!("game is not abortable")

  def resign(pov: Pov): ValidIOEvents =
    if (pov.game.resignable) finish(pov.game, Resign, Some(!pov.color))
    else !!("game is not resignable")

  def resignForce(pov: Pov): ValidIOEvents =
    if (pov.game.resignable && !pov.game.hasAi) 
      finish(pov.game, Timeout, Some(pov.color))
    else !!("game is not resignable")

  def drawClaim(pov: Pov): ValidIOEvents = pov match {
    case Pov(game, color) if game.playable && game.player.color == color && game.toChessHistory.threefoldRepetition ⇒ finish(game, Draw)
    case Pov(game, color) ⇒ !!("game is not threefold repetition")
  }

  def drawAccept(pov: Pov): ValidIOEvents =
    if (pov.opponent.isOfferingDraw)
      finish(pov.game, Draw, None, Some(_.drawOfferAccepted))
    else !!("opponent is not proposing a draw")

  def outoftime(game: DbGame): ValidIOEvents =
    game.outoftimePlayer some { player ⇒
      finish(game, Outoftime,
        Some(!player.color) filter game.toChess.board.hasEnoughMaterialToMate)
    } none !!("no outoftime applicable " + game.clock)

  def outoftimes(games: List[DbGame]): List[IO[Unit]] =
    games map { g ⇒
      outoftime(g).fold(
        msgs ⇒ putStrLn(g.id + " " + msgs.shows),
        _ map (_ ⇒ Unit) // events are lost
      ): IO[Unit]
    }

  def moveFinish(game: DbGame, color: Color): IO[List[Event]] =
    (game.status match {
      case Mate                        ⇒ finish(game, Mate, Some(color))
      case status @ (Stalemate | Draw) ⇒ finish(game, status)
      case _                           ⇒ success(io(Nil)): ValidIOEvents
    }) | io(Nil)

  private def finish(
    game: DbGame,
    status: Status,
    winner: Option[Color] = None,
    message: Option[SelectI18nKey] = None): Valid[IO[List[Event]]] =
    if (finisherLock isLocked game) !!("game finish is locked")
    else success(for {
      _ ← finisherLock lock game
      p1 = game.finish(status, winner)
      p2 ← message.fold(
        m ⇒ messenger.systemMessage(p1.game, m) map p1.++,
        io(p1)
      )
      _ ← gameRepo save p2
      winnerId = winner flatMap (p2.game.player(_).userId)
      _ ← gameRepo.finish(p2.game.id, winnerId)
      _ ← updateElo(p2.game)
      _ ← incNbGames(p2.game, White)
      _ ← incNbGames(p2.game, Black)
    } yield p2.events)

  private def incNbGames(game: DbGame, color: Color): IO[Unit] =
    game.player(color).userId.fold(
      id ⇒ userRepo.incNbGames(id, game.rated),
      io()
    )

  private def updateElo(game: DbGame): IO[Unit] =
    if (!game.finished || !game.rated || game.turns < 2) io()
    else {
      for {
        whiteUserId ← game.player(White).userId
        blackUserId ← game.player(Black).userId
        if whiteUserId != blackUserId
      } yield for {
        whiteUserOption ← userRepo byId whiteUserId
        blackUserOption ← userRepo byId blackUserId
        _ ← (whiteUserOption |@| blackUserOption).apply(
          (whiteUser, blackUser) ⇒ {
            val (whiteElo, blackElo) = eloCalculator.calculate(whiteUser, blackUser, game.winnerColor)
            for {
              _ ← gameRepo.setEloDiffs(
                game.id,
                whiteElo - whiteUser.elo,
                blackElo - blackUser.elo)
              _ ← eloUpdater.game(whiteUser, whiteElo, game.id)
              _ ← eloUpdater.game(blackUser, blackElo, game.id)
            } yield ()
          }
        ).fold(identity, io())
      } yield ()
    } | io()
}
