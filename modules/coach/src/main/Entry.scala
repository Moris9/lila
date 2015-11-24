package lila.coach

import chess.{ Color, Status }
import lila.rating.PerfType
import org.joda.time.DateTime

case class Entry(
  version: Int,
  userId: String,
  gameId: String,
  color: Color,
  perf: PerfType,
  eco: Ecopening,
  opponent: Opponent,
  cpl: Option[Grouped[Numbers]],
  movetime: Grouped[Numbers],
  luck: Option[Grouped[Ratio]],
  opportunism: Option[Grouped[Ratio]],
  nbMoves: Grouped[Int],
  result: Result,
  status: Status,
  endPhase: Phase,
  ratingDiff: Int,
  date: DateTime)

case class Opponent(rating: Int, strength: RelativeStrength)

sealed abstract class Result(val id: Int)
object Result {
  object Win extends Result(1)
  object Draw extends Result(2)
  object Loss extends Result(3)
  val all = List(Win, Draw, Loss)
}

sealed abstract class Phase(val id: Int)
object Phase {
  object Opening extends Phase(1)
  object Middle extends Phase(2)
  object End extends Phase(3)
  val all = List(Opening, Middle, End)
}

sealed abstract class RelativeStrength(val id: Int)
object RelativeStrength {
  object MuchWeaker extends RelativeStrength(10)
  object Weaker extends RelativeStrength(20)
  object Equal extends RelativeStrength(30)
  object Strong extends RelativeStrength(40)
  object MuchStronger extends RelativeStrength(50)
  val all = List(MuchWeaker, Weaker, Equal, Strong, MuchStronger)
}

case class ByMovetime[A](values: List[A])

object ByMovetime {
  val uppers = List(10, 30, 50, 100, 200, 6000)
}

case class ByPhase[A](opening: A, middle: Option[A], end: Option[A], all: A)

case class ByPieceRole[A](pawn: Option[A], knight: Option[A], bishop: Option[A], rook: Option[A], queen: Option[A], king: Option[A])

case class ByPositionQuality[A](losing: Option[A], bad: Option[A], equal: Option[A], good: Option[A], winning: Option[A])

case class Grouped[A](
  byPhase: ByPhase[A],
  byMovetime: ByMovetime[A],
  byPieceRole: ByPieceRole[A],
  byPositionQuality: ByPositionQuality[A])

case class Numbers(size: Int, mean: Int, median: Double, sd: Double)

object Numbers {
  def apply(nbs: List[Int]): Option[Numbers] = nbs match {
    case Nil => none
    case xs  => Numbers(nbs.size, 0, 0, 0).some
  }
}

case class Ratio(n: Int, d: Int) // n/d
