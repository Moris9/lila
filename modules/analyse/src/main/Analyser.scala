package lila.analyse

import akka.actor.ActorSelection

import chess.format.FEN
import lila.game.actorApi.InsertGame
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.AnalysisAvailable

final class Analyser(
    indexer: ActorSelection,
    roundSocket: ActorSelection,
    bus: lila.common.Bus) {

  def get(id: String): Fu[Option[Analysis]] = AnalysisRepo byId id

  def save(analysis: Analysis): Funit = GameRepo game analysis.id flatMap {
    _ ?? { game =>
      GameRepo.setAnalysed(game.id)
      AnalysisRepo.save(analysis) >>- {
        bus.publish(actorApi.AnalysisReady(game, analysis), 'analysisReady)
        roundSocket ! Tell(game.id, AnalysisAvailable)
        indexer ! InsertGame(game)
      }
    }
  }

  def progress(ratio: Float, analysis: Analysis): Funit =
    GameRepo gameWithInitialFen analysis.id map {
      _ ?? {
        case (game, initialFen) =>
          roundSocket ! Tell(analysis.id, actorApi.AnalysisProgress(
            ratio = ratio,
            analysis = analysis,
            pgnMoves = game.pgnMoves,
            variant = game.variant,
            initialFen = initialFen | FEN(game.variant.initialFen)))
      }
    }
}
