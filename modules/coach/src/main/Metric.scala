package lila.coach

sealed abstract class Metric(val key: String, val name: String)

object Metric {

  case object MeanCpl extends Metric("meanCpl", "Mean CPL")
  case object Movetime extends Metric("movetime", "Move time")
  case object Result extends Metric("result", "Result")
  case object RatingDiff extends Metric("ratingDiff", "Rating gain")
  case object NbMoves extends Metric("nbMoves", "Number of moves")
}
