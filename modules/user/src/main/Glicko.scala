package lila.user

import reactivemongo.bson.BSONDocument

import lila.db.BSON

case class Glicko(
    rating: Double,
    deviation: Double,
    volatility: Double) {

  def intRating = rating.toInt
  def intDeviation = deviation.toInt
  def intDeviationDoubled = (deviation * 2).toInt

  def interval = (rating - deviation * 2).toInt -> (rating + deviation * 2).toInt

  override def toString = s"$intRating $intDeviation"
}

case object Glicko {

  val default = Glicko(1500d, 350d, 0.06d)

  def range(rating: Double, deviation: Double) = (
    rating - (deviation * 2),
    rating + (deviation * 2)
  )

  private def GlickoBSONHandler = new BSON[Glicko] {

    def reads(r: BSON.Reader): Glicko = Glicko(
      rating = r double "r",
      deviation = r double "d",
      volatility = r double "v")

    def writes(w: BSON.Writer, o: Glicko) = BSONDocument(
      "r" -> w.double(o.rating),
      "d" -> w.double(o.deviation),
      "v" -> w.double(o.volatility))
  }

  sealed abstract class Result(val v: Double) {
    def negate: Result
  }
  object Result {
    case object Win extends Result(1) { def negate = Loss }
    case object Loss extends Result(0) { def negate = Win }
    case object Draw extends Result(0.5) { def negate = Draw }
  }

  lazy val tube = lila.db.BsTube(GlickoBSONHandler)
}
