package lila.insight

import play.twirl.api.Html
import reactivemongo.bson._

sealed abstract class Metric(
  val key: String,
  val name: String,
  val dbKey: String,
  val position: Position,
  val per: Position,
  val dataType: Metric.DataType,
  val description: Html)

object Metric {

  sealed trait DataType {
    def name = toString.toLowerCase
  }
  object DataType {
    case object Seconds extends DataType
    case object Count extends DataType
    case object Average extends DataType
    case object Percent extends DataType
  }

  import BSONHandlers._
  import DataType._
  import Position._
  import Entry.{ BSONFields => F }

  case object MeanCpl extends Metric("acpl", "Average centipawn loss", F moves "c", Move, Move, Average,
    Html("""Precision of your moves. Lower is better. <a href="http://lichess.org/qa/103/what-is-average-centipawn-loss">More info</a>"""))

  case object Movetime extends Metric("movetime", "Move time", F moves "t", Move, Move, Seconds,
    Dimension.MovetimeRange.description)

  case object Result extends Metric("result", "Game result", F.result, Game, Game, Percent,
    Dimension.Result.description)

  case object Termination extends Metric("termination", "Game termination", F.termination, Game, Game, Percent,
    Dimension.Termination.description)

  case object RatingDiff extends Metric("ratingDiff", "Rating gain", F.ratingDiff, Game, Game, Average,
    Html("The amount of rating points you win or lose when the game ends."))

  case object OpponentRating extends Metric("opponentRating", "Opponent rating", F.opponentRating, Game, Game, Average,
    Html("The average rating of your opponent for the relevant variant."))

  case object NbMoves extends Metric("nbMoves", "Moves per game", F moves "r", Move, Game, Average,
    Html("Number of moves you play in the game. Doesn't count the opponent moves."))

  case object PieceRole extends Metric("piece", "Piece moved", F moves "r", Move, Move, Percent,
    Dimension.PieceRole.description)

  case object Opportunism extends Metric("opportunism", "Opportunism", F moves "o", Move, Move, Percent,
    Html("How often you take advantage of your opponent blunders. 100% means you punish them all, 0% means you counter-blunder them all."))

  case object Luck extends Metric("luck", "Luck", F moves "l", Move, Move, Percent,
    Html("How often your opponent fails to punish your blunders. 100% means they miss all your blunders, 0% means they spot them all."))

  case object Material extends Metric("material", "Material imbalance", F moves "i", Move, Move, Average,
    Dimension.MaterialRange.description)

  val all = List(MeanCpl, Movetime, Result, Termination, RatingDiff, OpponentRating, NbMoves, PieceRole, Opportunism, Luck, Material)
  val byKey = all map { p => (p.key, p) } toMap

  def requiresAnalysis(m: Metric) = m match {
    case MeanCpl => true
    case _       => false
  }

  def requiresStableRating(m: Metric) = m match {
    case RatingDiff     => true
    case OpponentRating => true
    case _              => false
  }

  def isStacked(m: Metric) = m match {
    case Result      => true
    case Termination => true
    case PieceRole   => true
    case _           => false
  }

  def valuesOf(metric: Metric): List[MetricValue] = metric match {
    case Result => lila.insight.Result.all.map { r =>
      MetricValue(BSONInteger(r.id), MetricValueName(r.name))
    }
    case Termination => lila.insight.Termination.all.map { r =>
      MetricValue(BSONInteger(r.id), MetricValueName(r.name))
    }
    case PieceRole => chess.Role.all.reverse.map { r =>
      MetricValue(BSONString(r.forsyth.toString), MetricValueName(r.toString))
    }
    case _ => Nil
  }

  case class MetricValueName(name: String)
  case class MetricValue(key: BSONValue, name: MetricValueName)
}
