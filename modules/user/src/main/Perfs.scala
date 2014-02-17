package lila.user

import reactivemongo.bson.BSONDocument

import lila.rating.Perf
import lila.db.BSON

case class Perfs(
    global: Perf,
    standard: Perf,
    chess960: Perf,
    bullet: Perf,
    blitz: Perf,
    slow: Perf,
    white: Perf,
    black: Perf,
    puzzle: Perf) {

  def perfs = List(
    "global" -> global,
    "standard" -> standard,
    "chess960" -> chess960,
    "bullet" -> bullet,
    "blitz" -> blitz,
    "slow" -> slow,
    "white" -> white,
    "black" -> black,
    "puzzle" -> puzzle)

  override def toString = perfs map {
    case (name, perf) => s"$name:${perf.intRating}"
  } mkString ", "
}

case object Perfs {

  val default = {
    val p = Perf.default
    Perfs(p, p, p, p, p, p, p, p, p)
  }

  val titles = Map(
    "bullet"   -> "Very fast games: less than 3 minutes",
    "blitz"    -> "Fast games: less than 8 minutes",
    "slow"     -> "Slow games: more than 8 minutes",
    "standard" -> "Standard rules of chess",
    "chess960" -> "Chess960 variant",
    "white"    -> "With white pieces",
    "black"    -> "With black pieces",
    "puzzle"   -> "Training puzzles")

  private def PerfsBSONHandler = new BSON[Perfs] {

    implicit def perfHandler = Perf.tube.handler

    def reads(r: BSON.Reader): Perfs = {
      def perf(key: String) = r.getO[Perf](key) getOrElse Perf.default
      Perfs(
        global = perf("global"),
        standard = perf("standard"),
        chess960 = perf("chess960"),
        bullet = perf("bullet"),
        blitz = perf("blitz"),
        slow = perf("slow"),
        white = perf("white"),
        black = perf("black"),
        puzzle = perf("puzzle"))
    }

    def writes(w: BSON.Writer, o: Perfs) = BSONDocument(
      "global" -> o.global,
      "standard" -> o.standard,
      "chess960" -> o.chess960,
      "bullet" -> o.bullet,
      "blitz" -> o.blitz,
      "slow" -> o.slow,
      "white" -> o.white,
      "black" -> o.black,
      "puzzle" -> o.puzzle)
  }

  lazy val tube = lila.db.BsTube(PerfsBSONHandler)
}
