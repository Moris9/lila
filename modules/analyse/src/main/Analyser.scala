package lila.analyse

import akka.actor.ActorSelection

import chess.format.FEN
import lila.common.Bus
import lila.game.actorApi.InsertGame
import lila.game.{ GameRepo, Game }
import lila.hub.actorApi.map.TellIfExists
import lila.hub.actors.GameSearch

final class Analyser(
    indexer: GameSearch,
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo,
    requesterApi: RequesterApi
) {

  def get(game: Game): Fu[Option[Analysis]] =
    analysisRepo byGame game

  def byId(id: Analysis.ID): Fu[Option[Analysis]] = analysisRepo byId id

  def save(analysis: Analysis): Funit = analysis.studyId match {
    case None => gameRepo game analysis.id flatMap {
      _ ?? { game =>
        gameRepo.setAnalysed(game.id)
        analysisRepo.save(analysis) >>
          sendAnalysisProgress(analysis, complete = true) >>- {
            Bus.publish(actorApi.AnalysisReady(game, analysis), "analysisReady")
            Bus.publish(InsertGame(game), "gameSearchInsert")
            requesterApi save analysis
          }
      }
    }
    case Some(studyId) =>
      analysisRepo.save(analysis) >>
        sendAnalysisProgress(analysis, complete = true) >>- {
          requesterApi save analysis
        }
  }

  def progress(analysis: Analysis): Funit = sendAnalysisProgress(analysis, complete = false)

  private def sendAnalysisProgress(analysis: Analysis, complete: Boolean): Funit = analysis.studyId match {
    case None => gameRepo gameWithInitialFen analysis.id map {
      _ ?? {
        case (game, initialFen) => Bus.publish(
          TellIfExists(analysis.id, actorApi.AnalysisProgress(
            analysis = analysis,
            game = game,
            variant = game.variant,
            initialFen = initialFen | FEN(game.variant.initialFen)
          )),
          "roundSocket"
        )
      }
    }
    case Some(studyId) => fuccess {
      Bus.publish(actorApi.StudyAnalysisProgress(analysis, complete), "studyAnalysisProgress")
    }
  }
}
