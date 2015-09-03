package lila.analyse

import scala.concurrent.Future
import scala.util.{ Success, Failure }

import akka.actor.ActorSelection
import chess.format.UciDump

import lila.db.api._
import lila.game.actorApi.InsertGame
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo }
import tube.analysisTube

case class ConcurrentAnalysisException(gameId: String, userId: String, userIp: Option[String]) extends Exception {
  override def getMessage = s"analysis limiter: discard http://lichess.org/$gameId user:$userId IP:$userIp"
}

final class Analyser(
    ai: ActorSelection,
    indexer: ActorSelection,
    modActor: ActorSelection,
    limiter: Limiter) {

  def get(id: String): Fu[Option[Analysis]] = AnalysisRepo byId id flatMap evictStalled
  def getNotDone(id: String): Fu[Option[Analysis]] = AnalysisRepo notDoneById id flatMap evictStalled

  def getDone(id: String): Fu[Option[Analysis]] = AnalysisRepo doneById id flatMap evictStalled

  def evictStalled(oa: Option[Analysis]): Fu[Option[Analysis]] = oa ?? { a =>
    if (a.stalled) (AnalysisRepo remove a.id) inject none[Analysis] else fuccess(a.some)
  }

  def getOrGenerate(
    id: String,
    userId: String,
    userIp: Option[String],
    concurrent: Boolean,
    auto: Boolean): Fu[Analysis] = {

    def generate: Fu[Analysis] =
      if (concurrent || userIp.?? { ip =>
        limiter(gameId = id, ip = ip, user = userId)
      }) doGenerate
      else fufail(ConcurrentAnalysisException(id, userId, userIp))

    def doGenerate: Fu[Analysis] =
      $find.byId[Game](id) flatMap {
        case Some(game) if game.analysable => GameRepo initialFen game flatMap { initialFen =>
          AnalysisRepo.progress(id, userId, game.startedAtTurn) >> {
            chess.Replay(game.pgnMoves, initialFen, game.variant).fold(
              fufail(_),
              replay => {
                ai ! lila.hub.actorApi.ai.Analyse(game.id, UciDump(replay), initialFen, requestedByHuman = !auto, game.variant)
                AnalysisRepo byId id flatten "Missing analysis"
              }
            )
          }
        }
        case Some(game) => fufail(s"[analysis] game $id is not analysable")
        case _          => fufail(s"[analysis] game $id is missing")
      }

    get(id) flatMap {
      _.fold(generate)(fuccess)
    }
  }

  def complete(id: String, data: String, fromIp: String) = {
    limiter release id
    $find.byId[Game](id) zip get(id) zip (GameRepo initialFen id) flatMap {
      case ((Some(game), Some(a1)), initialFen) if game.analysable =>
        Info.decodeList(data, game.startedAtTurn) match {
          case None => fufail(s"[analysis] $data")
          case Some(infos) => chess.Replay(game.pgnMoves, initialFen, game.variant).fold(
            fufail(_),
            replay => UciToPgn(replay, a1 complete infos) match {
              case (analysis, errors) =>
                errors foreach { e => logwarn(s"[analysis UciToPgn] $id $e") }
                if (analysis.valid) {
                  if (analysis.emptyRatio >= 1d / 10)
                    fufail(s"Analysis $id from $fromIp has ${analysis.nbEmptyInfos} empty infos out of ${analysis.infos.size}")
                  indexer ! InsertGame(game)
                  AnalysisRepo.done(id, analysis, fromIp) >>- {
                    modActor ! actorApi.AnalysisReady(game, analysis)
                  } >>- GameRepo.setAnalysed(game.id) inject analysis
                }
                else fufail(s"[analysis] invalid analysis ${analysis}\nwith errors $errors")
            })
        }
      case ((Some(game), _), _) => fufail(s"[analysis] complete non analysable $id")
      case _                    => fufail(s"[analysis] complete non existing $id")
    } addFailureEffect {
      _ => AnalysisRepo remove id
    }
  }

  def completeErr(id: String, err: String, fromIp: String) =
    $find.byId[Game](id) zip getNotDone(id) flatMap {
      case (Some(game), Some(a1)) if game.analysable => AnalysisRepo remove id
    }
}
