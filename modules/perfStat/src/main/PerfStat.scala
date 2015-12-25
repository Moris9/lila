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
    bestWin: Option[Result],
    worstLoss: Option[Result],
    count: Count,
    resultStreak: ResultStreak,
    playStreak: PlayStreak) {

  def id = _id

  def agg(pov: Pov) = if (!pov.game.finished) this else copy(
    highest = RatingAt.agg(highest, pov, 1),
    lowest = RatingAt.agg(lowest, pov, -1),
    bestWin = (~pov.win).fold(Result.agg(bestWin, pov, 1), bestWin),
    worstLoss = (~pov.loss).fold(Result.agg(worstLoss, pov, -1), worstLoss),
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
    bestWin = none,
    worstLoss = none,
    count = Count(all = 0, rated = 0, win = 0, loss = 0, draw = 0, opAvg = Avg(0, 0)),
    resultStreak = ResultStreak(curWin = 0, curLoss = 0, maxWin = 0, maxLoss = 0),
    playStreak = PlayStreak(curNb = 0, curSeconds = 0, maxNb = 0, maxSeconds = 0, lastDate = none)
  )
}

case class ResultStreak(
    curWin: Int,
    curLoss: Int,
    maxWin: Int,
    maxLoss: Int) {
  def agg(pov: Pov) = copy(
    curWin = (~pov.win).fold(curWin + 1, 0),
    curLoss = (~pov.loss).fold(curLoss + 1, 0)).setMax
  private def setMax = copy(
    maxWin = curWin max maxWin,
    maxLoss = curLoss max maxLoss)
}

case class PlayStreak(
    curNb: Int,
    curSeconds: Int,
    maxNb: Int,
    maxSeconds: Int,
    lastDate: Option[DateTime]) {
  def agg(pov: Pov) = {
    val cont = lastDate.fold(true) { ld => pov.game.createdAt.isBefore(ld plusMinutes 60) }
    val seconds = (pov.game.updatedAtOrCreatedAt.getSeconds - pov.game.createdAt.getSeconds).toInt
    copy(
      curNb = cont.fold(curNb, 0) + 1,
      curSeconds = cont.fold(curSeconds, 0) + seconds,
      lastDate = pov.game.updatedAtOrCreatedAt.some
    ).setMax
  }
  private def setMax = copy(
    maxNb = curNb max maxNb,
    maxSeconds = curSeconds max maxSeconds)
}

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
object Result {
  def agg(cur: Option[Result], pov: Pov, comp: Int) =
    pov.opponent.rating.filter { r =>
      cur.fold(true) { c => r.compare(c.opInt) == comp }
    }.map {
      Result(_, UserId(~pov.opponent.userId), pov.game.updatedAtOrCreatedAt, pov.game.id)
    } orElse cur
}

case class UserId(value: String)
