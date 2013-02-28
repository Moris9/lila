package lila.app
package ai

import chess.{ Game, Move }
import game.DbGame
import analyse.Analysis

import scala.concurrent.Future

trait Ai {

  def play(dbGame: DbGame, pgn: String, initialFen: Option[String]): Future[Valid[(Game, Move)]]

  def analyse(pgn: String, initialFen: Option[String]): Future[Valid[Analysis]]
}
