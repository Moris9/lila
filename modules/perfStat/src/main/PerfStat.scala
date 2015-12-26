package lila.perfStat

import lila.game.Pov
import lila.rating.PerfType

import org.joda.time.DateTime

case class PerfStat(
    _id: String, // userId/perfId
    userId: UserId,
    perfType: PerfType,
    highest: Option[RatingAt],
    lowest: Option[RatingAt],
    bestWins: Results,
    worstLosses: Results,
    count: Count,
    resultStreak: ResultStreak,
    playStreak: PlayStreak) {

  def id = _id

  def agg(pov: Pov) = if (!pov.game.finished) this else copy(
    highest = RatingAt.agg(highest, pov, 1),
    lowest = RatingAt.agg(lowest, pov, -1),
    bestWins = (~pov.win).fold(bestWins.agg(pov, -1), bestWins),
    worstLosses = (~pov.loss).fold(worstLosses.agg(pov, 1), worstLosses),
    count = count(pov),
    resultStreak = resultStreak agg pov,
    playStreak = playStreak agg pov
  )
}

object PerfStat {

  def makeId(userId: String, perfType: PerfType) = s"$userId/${perfType.id}"

  def init(userId: String, perfType: PerfType) = PerfStat(
    _id = makeId(userId, perfType),
    userId = UserId(userId),
    perfType = perfType,
    highest = none,
    lowest = none,
    bestWins = Results(Nil),
    worstLosses = Results(Nil),
    count = Count(all = 0, rated = 0, win = 0, loss = 0, draw = 0, opAvg = Avg(0, 0)),
    resultStreak = ResultStreak(win = Streaks.init, loss = Streaks.init),
    playStreak = PlayStreak(nb = Streaks.init, time = Streaks.init, lastDate = none)
  )
}

case class ResultStreak(win: Streaks, loss: Streaks) {
  def agg(pov: Pov) = copy(win = win(~pov.win, pov)(1), loss = loss(~pov.loss, pov)(1))
}

case class PlayStreak(nb: Streaks, time: Streaks, lastDate: Option[DateTime]) {
  def agg(pov: Pov) = {
    val cont = lastDate.fold(true) { ld => pov.game.createdAt.isBefore(ld plusMinutes 60) }
    val seconds = (pov.game.updatedAtOrCreatedAt.getSeconds - pov.game.createdAt.getSeconds).toInt
    copy(
      nb = nb(cont, pov)(1),
      time = time(cont, pov)(seconds),
      lastDate = pov.game.updatedAtOrCreatedAt.some)
  }
}

case class Streaks(cur: Streak, max: Streak) {
  def apply(cont: Boolean, pov: Pov)(v: Int) = copy(
    cur = cur(cont, pov)(v)
  ).setMax
  private def setMax = copy(max = if (cur.v >= max.v) cur else max)
}
object Streaks {
  val init = Streaks(Streak.init, Streak.init)
}
case class Streak(v: Int, from: Option[RatingAt], to: Option[RatingAt]) {
  def apply(cont: Boolean, pov: Pov)(v: Int) = cont.fold(inc _, reset _)(pov, v)
  private def inc(pov: Pov, by: Int) = copy(
    v = v + by,
    to = pov.player.ratingAfter.map { RatingAt(_, pov.game.updatedAtOrCreatedAt, pov.gameId) })
  private def reset(pov: Pov, to: Int) = copy(
    v = to,
    from = pov.player.rating.map { RatingAt(_, pov.game.createdAt, pov.gameId) },
    to = none)
}
object Streak {
  val init = Streak(0, none, none)
}

case class Bounds(from: RatingAt, to: RatingAt)

case class Count(
    all: Int,
    rated: Int,
    win: Int,
    loss: Int,
    draw: Int,
    opAvg: Avg) {
  def apply(pov: Pov) = copy(
    all = all + 1,
    rated = rated + pov.game.rated.fold(1, 0),
    win = win + pov.win.contains(true).fold(1, 0),
    loss = loss + pov.win.contains(false).fold(1, 0),
    draw = draw + pov.win.isEmpty.fold(1, 0),
    opAvg = pov.opponent.stableRating.fold(opAvg)(opAvg.agg))
}

case class Avg(avg: Double, pop: Int) {
  def agg(v: Int) = copy(
    avg = ((avg * pop) + v) / (pop + 1),
    pop = pop + 1)
}

case class RatingAt(int: Int, at: DateTime, gameId: String)
object RatingAt {
  def agg(cur: Option[RatingAt], pov: Pov, comp: Int) =
    pov.player.stableRatingAfter.filter { r =>
      cur.fold(true) { c => r.compare(c.int) == comp }
    }.map {
      RatingAt(_, pov.game.updatedAtOrCreatedAt, pov.game.id)
    } orElse cur
}

case class Result(opInt: Int, opId: UserId, at: DateTime, gameId: String)

case class Results(results: List[Result]) {
  def agg(pov: Pov, comp: Int) = pov.opponent.rating.fold(this) { opInt =>
    copy(
      results = (Result(
        opInt,
        UserId(~pov.opponent.userId),
        pov.game.updatedAtOrCreatedAt,
        pov.game.id
      ) :: results).sortBy(_.opInt * comp) take Results.nb)
  }
}
object Results {
  val nb = 5
}

case class UserId(value: String)
