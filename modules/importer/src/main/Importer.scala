package lila.importer

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.ActorRef
import akka.pattern.{ ask, after }
import chess.{ Color, MoveOrDrop, Status }
import makeTimeout.large

import lila.db.api._
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._

final class Importer(
    roundMap: ActorRef,
    delay: FiniteDuration,
    scheduler: akka.actor.Scheduler) {

  def apply(data: ImportData, user: Option[String], forceId: Option[String] = None): Fu[Game] = {

    def gameExists(processing: => Fu[Game]): Fu[Game] =
      GameRepo.findPgnImport(data.pgn) flatMap { _.fold(processing)(fuccess) }

    def applyResult(game: Game, result: Result) {
      result match {
        case Result(Status.Draw, _)             => roundMap ! Tell(game.id, ImportDraw)
        case Result(Status.Resign, Some(color)) => roundMap ! Tell(game.id, ImportResign(!color))
        case _                                  =>
      }
    }

    def applyMoves(pov: Pov, moves: List[MoveOrDrop]): Funit = moves match {
      case Nil => after(delay, scheduler)(funit)
      case move :: rest =>
        after(delay, scheduler)(Future(applyMove(pov, move))) >> applyMoves(!pov, rest)
    }

    def applyMove(pov: Pov, move: MoveOrDrop) {
      roundMap ! Tell(pov.gameId, ImportPlay(
        playerId = pov.playerId,
        move.fold(_.toUci, _.toUci)))
    }

    gameExists {
      (data preprocess user).future flatMap {
        case Preprocessed(g, moves, result) =>
          val game = forceId.fold(g)(g.withId)
          (GameRepo insertDenormalized game) >> {
            game.pgnImport.flatMap(_.user).isDefined ?? GameRepo.setImportCreatedAt(game)
          } >> applyMoves(Pov(game, Color.white), moves) >>-
            (result foreach { r => applyResult(game, r) }) >>
            (GameRepo game game.id).map(_ | game)
      }
    }
  }
}
