package lila.round

import akka.actor.{ Cancellable, Scheduler }
import scala.concurrent.duration._

import chess.Color
import lila.game.{ Game, GameRepo, Pov, Progress }
import ornicar.scalalib.Zero

final private class GameProxy(
    id: Game.ID,
    dependencies: GameProxy.Dependencies
)(implicit ec: scala.concurrent.ExecutionContext) {

  import GameProxy._
  import dependencies._

  def game: Fu[Option[Game]] = cache

  def save(progress: Progress): Funit = {
    set(progress.game)
    dirtyProgress = dirtyProgress.fold(progress.dropEvents)(_ withGame progress.game).some
    if (shouldFlushProgress(progress)) flushProgress
    else fuccess(scheduleFlushProgress)
  }

  private def set(game: Game): Unit = {
    cache = fuccess(game.some)
  }

  private[round] def setFinishedGame(game: Game): Unit = {
    scheduledFlush.cancel()
    set(game)
    dirtyProgress = none
  }

  // convenience helpers

  def pov(color: Color) = game.dmap {
    _ map { Pov(_, color) }
  }

  def playerPov(playerId: String) = game.dmap {
    _ flatMap { Pov(_, playerId) }
  }

  def withGame[A: Zero](f: Game => Fu[A]): Fu[A] = game.flatMap(_ ?? f)

  // internals

  private var dirtyProgress: Option[Progress] = None
  private var scheduledFlush: Cancellable     = emptyCancellable

  private def shouldFlushProgress(p: Progress) =
    alwaysPersist() || p.statusChanged || p.game.isSimul || (
      p.game.hasCorrespondenceClock && !p.game.hasAi && p.game.rated
    )

  private def scheduleFlushProgress() = {
    scheduledFlush.cancel()
    scheduledFlush = scheduler.scheduleOnce(scheduleDelay)(flushProgress)
  }

  private def flushProgress = {
    scheduledFlush.cancel()
    dirtyProgress ?? gameRepo.update addEffect { _ =>
      dirtyProgress = none
    }
  }

  private[this] var cache: Fu[Option[Game]] = fetch

  private[this] def fetch = gameRepo game id
}

private object GameProxy {

  class Dependencies(
      val gameRepo: GameRepo,
      val alwaysPersist: () => Boolean,
      val scheduler: Scheduler
  )

  // must be way under round.active.ttl = 40 seconds
  private val scheduleDelay = 20.seconds

  private val emptyCancellable = new Cancellable {
    def cancel()    = true
    def isCancelled = true
  }
}
