package lila.tutor

import alleycats.Zero
import chess.{ Division, Situation }

import lila.analyse.Analysis
import lila.game.Pov
import lila.insight.MeanRating
import lila.rating.PerfType
import lila.user.User

case class TutorMetric[A](mine: A, peer: Option[A])(implicit o: Ordering[A]) {
  def higher = peer.exists(p => o.compare(mine, p) >= 0)
}
case class TutorMetricOption[A](mine: Option[A], peer: Option[A])(implicit o: Ordering[A]) {
  def higher = mine.exists(m => peer.exists(p => o.compare(m, p) >= 0))
}

case class TutorRatio(value: Double) extends AnyVal

object TutorRatio {

  def apply(a: Int, b: Int): TutorRatio = TutorRatio(a.toDouble / b)

  implicit val zero     = Zero(TutorRatio(0))
  implicit val ordering = Ordering.by[TutorRatio, Double](_.value)
}

case class TutorUser(user: User, rating: MeanRating)
