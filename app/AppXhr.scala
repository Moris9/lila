package lila

import model._
import memo._
import db.{ GameRepo }
import chess._
import Pos.posAt
import scalaz.effects._

final class AppXhr(
    gameRepo: GameRepo,
    messenger: Messenger,
    ai: () ⇒ Ai,
    finisher: Finisher,
    aliveMemo: AliveMemo,
    moretimeSeconds: Int) {

  type IOValid = IO[Valid[Unit]]

  def play(
    fullId: String,
    origString: String,
    destString: String,
    promString: Option[String] = None): List[Event] = fromPov(fullId) {
    case Pov(g1, color) ⇒ (for {
      g2 ← (g1.playable).fold(success(g1), failure("Game not playable" wrapNel))
      orig ← posAt(origString) toValid "Wrong orig " + origString
      dest ← posAt(destString) toValid "Wrong dest " + destString
      promotion ← Role promotable promString toValid "Wrong promotion"
      newChessGameAndMove ← g2.toChess(orig, dest, promotion)
      (newChessGame, move) = newChessGameAndMove
    } yield g2.update(newChessGame, move)).fold(
      e ⇒ io(failure(e)),
      progress ⇒ for {
        _ ← aliveMemo.put(progress.game.id, color)
        events ← if (progress.game.finished) for {
          _ ← gameRepo save progress
          finishEvents ← finisher.moveFinish(progress.game, color)
        } yield progress.events ::: finishEvents
        else if (progress.game.player.isAi && progress.game.playable) for {
          aiResult ← ai()(progress.game) map (_.err)
          (newChessGame, move) = aiResult
          progress2 = progress flatMap { _.update(newChessGame, move) }
          _ ← gameRepo save progress2
          finishEvents ← finisher.moveFinish(progress2.game, !color)
        } yield progress2.events ::: finishEvents
        else for {
          _ ← gameRepo save progress
        } yield progress.events
      } yield success(events)
    )
  }

  def abort(fullId: String): IOValid = attempt(fullId, finisher.abort)

  def resign(fullId: String): IOValid = attempt(fullId, finisher.resign)

  def forceResign(fullId: String): IOValid = attempt(fullId, finisher.forceResign)

  def outoftime(fullId: String): IOValid = attempt(fullId, finisher outoftime _.game)

  def drawClaim(fullId: String): IOValid = attempt(fullId, finisher.drawClaim)

  def drawAccept(fullId: String): IOValid = attempt(fullId, finisher.drawAccept)

  def drawOffer(fullId: String): IO[Valid[List[Event]]] = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (g1 playerCanOfferDraw color) {
        if (g1.player(!color).isOfferingDraw) finisher drawAccept pov
        else success {
          for {
            p1 ← messenger.systemMessages(g1, "Draw offer sent") map { es ⇒
              Progress(g1, ReloadTableEvent(!color) :: es)
            }
            p2 = p1 map { g ⇒ g.updatePlayer(color, _ offerDraw g.turns) }
            _ ← gameRepo save p2
          } yield p2.events
        }
      }
      else !!("invalid draw offer " + fullId)
  })

  def drawCancel(fullId: String): IO[Valid[List[Event]]] = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (pov.player.isOfferingDraw) success {
        for {
          p1 ← messenger.systemMessages(g1, "Draw offer canceled") map { es ⇒
            Progress(g1, ReloadTableEvent(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeDrawOffer) }
          _ ← gameRepo save p2
        } yield p2.events
      }
      else !!("no draw offer to cancel " + fullId)
  })

  def drawDecline(fullId: String): IO[Valid[List[Event]]] = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (g1.player(!color).isOfferingDraw) success {
        for {
          p1 ← messenger.systemMessages(g1, "Draw offer declined") map { es ⇒
            Progress(g1, ReloadTableEvent(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeDrawOffer) }
            _ ← gameRepo save p2
          } yield p2.events
      }
      else !!("no draw offer to decline " + fullId)
  })

  def moretime(fullId: String): IO[Valid[Float]] = attempt(fullId, pov ⇒
    pov.game.clock filter (_ ⇒ pov.game.playable) map { clock ⇒
      val color = !pov.color
      val newClock = clock.giveTime(color, moretimeSeconds)
      val g2 = pov.game withClock newClock
      for {
        progress ← messenger.systemMessage(
          g2, "%s + %d seconds".format(color, moretimeSeconds)
        ) map { es ⇒
            Progress(pov.game, MoretimeEvent(color, moretimeSeconds) :: es)
          }
        _ ← gameRepo save progress
      } yield newClock remainingTime color
    } toValid "cannot add moretime"
  )

  private def attempt[A](
    fullId: String,
    action: Pov ⇒ Valid[IO[A]]): IO[Valid[A]] =
    fromPov(fullId) { pov ⇒ action(pov).sequence }

  private def fromPov[A](fullId: String)(op: Pov ⇒ IO[A]): IO[A] =
    gameRepo pov fullId flatMap op

  private def !!(msg: String) = failure(msg.wrapNel)
}
