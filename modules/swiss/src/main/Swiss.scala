package lila.swiss

import chess.Clock.{ Config => ClockConfig }
import chess.Speed
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.game.PerfPicker
import lila.hub.LightTeam.TeamID
import lila.rating.PerfType
import lila.user.User

case class Swiss(
    _id: Swiss.Id,
    name: String,
    clock: ClockConfig,
    variant: chess.variant.Variant,
    rated: Boolean,
    round: SwissRound.Number, // ongoing round
    nbRounds: Int,
    nbPlayers: Int,
    createdAt: DateTime,
    createdBy: User.ID,
    teamId: TeamID,
    startsAt: DateTime,
    finishedAt: Option[DateTime],
    winnerId: Option[User.ID] = None,
    description: Option[String] = None,
    hasChat: Boolean = true
) {
  def id = _id

  def isCreated      = round.value == 0
  def isStarted      = !isCreated && !isFinished
  def isFinished     = finishedAt.isDefined
  def isEnterable    = !isFinished
  def isNowOrSoon    = startsAt.isBefore(DateTime.now plusMinutes 15) && !isFinished
  def secondsToStart = (startsAt.getSeconds - nowSeconds).toInt atLeast 0
  // def isRecentlyFinished = finishedAt.exists(f => (nowSeconds - f.getSeconds) < 30 * 60)

  def allRounds: List[SwissRound.Number]      = (1 to round.value).toList.map(SwissRound.Number.apply)
  def finishedRounds: List[SwissRound.Number] = (1 to (round.value - 1)).toList.map(SwissRound.Number.apply)

  def speed = Speed(clock)

  def perfType: Option[PerfType] = PerfPicker.perfType(speed, variant, none)
  def perfLens                   = PerfPicker.mainOrDefault(speed, variant, none)

  def estimatedDuration: FiniteDuration = {
    (clock.limit.toSeconds + clock.increment.toSeconds * 80 + 10) * nbRounds
  }.toInt.seconds

  def estimatedDurationString = {
    val minutes = estimatedDuration.toMinutes
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${(minutes % 60)}m" else "")
  }
}

object Swiss {

  case class Id(value: String) extends AnyVal with StringValue
  case class Round(value: Int) extends AnyVal with IntValue

  case class Points(double: Int) extends AnyVal {
    def value: Float = double / 2f
  }
  case class Score(double: Int) extends AnyVal {
    def value: Float = double / 2f
  }

  def makeId = Id(scala.util.Random.alphanumeric take 8 mkString)
}
