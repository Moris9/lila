package lila.rating

case object RatingRegulator {

  def apply(factors: RatingFactors)(perfType: PerfType, before: Perf, after: Perf): Perf =
    factors.get(perfType).filter(1!=).fold(after) {
      apply(_, before, after)
    }

  def apply(factor: RatingFactor, before: Perf, after: Perf): Perf =
    if ({
      (after.nb == before.nb + 1) && // after playing one game
        (after.glicko.rating > before.glicko.rating) // and gaining rating
    }) {
      val diff = (after.glicko.rating - before.glicko.rating).abs
      val extra = diff * factor.value
      after.copy(
        glicko = after.glicko.copy(
          rating = after.glicko.rating + extra
        )
      )
    } else after
}
