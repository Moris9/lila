package lila.swiss

import chess.Clock.{ Config as ClockConfig }
import chess.format.FEN
import chess.Speed
import org.joda.time.DateTime
import scala.concurrent.duration.*

import lila.rating.PerfType
import lila.user.User

case class Swiss(
    _id: SwissId,
    name: String,
    clock: ClockConfig,
    variant: chess.variant.Variant,
    round: SwissRoundNumber, // ongoing round
    nbPlayers: Int,
    nbOngoing: Int,
    createdAt: DateTime,
    createdBy: User.ID,
    teamId: TeamId,
    startsAt: DateTime,
    settings: Swiss.Settings,
    nextRoundAt: Option[DateTime],
    finishedAt: Option[DateTime],
    winnerId: Option[User.ID] = None
):
  inline def id = _id

  def isCreated          = round.value == 0
  def isStarted          = !isCreated && !isFinished
  def isFinished         = finishedAt.isDefined
  def isNotFinished      = !isFinished
  def isNowOrSoon        = startsAt.isBefore(DateTime.now plusMinutes 15) && !isFinished
  def isRecentlyFinished = finishedAt.exists(f => (nowSeconds - f.getSeconds) < 30 * 60)
  def isEnterable =
    isNotFinished && round.value <= settings.nbRounds / 2 && nbPlayers < Swiss.maxPlayers

  def allRounds: List[SwissRoundNumber]      = SwissRoundNumber from (1 to round.value).toList
  def finishedRounds: List[SwissRoundNumber] = SwissRoundNumber from (1 until round.value).toList

  def startRound =
    copy(
      round = SwissRoundNumber(round.value + 1),
      nextRoundAt = none
    )

  def speed = Speed(clock)

  def perfType: PerfType = PerfType(variant, speed)

  def estimatedDuration: FiniteDuration = {
    (clock.limit.toSeconds + clock.increment.toSeconds * 80 + 10) * settings.nbRounds
  }.toInt.seconds

  def estimatedDurationString =
    val minutes = estimatedDuration.toMinutes
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${minutes % 60}m" else "")

  def roundInfo = Swiss.RoundInfo(teamId, settings.chatFor)

  def withConditions(conditions: SwissCondition.All) = copy(
    settings = settings.copy(conditions = conditions)
  )

  def unrealisticSettings =
    !settings.manualRounds &&
      settings.dailyInterval.isEmpty &&
      clock.estimateTotalSeconds * 2 * settings.nbRounds > 3600 * 8

  lazy val looksLikePrize = lila.common.String.looksLikePrize(s"$name ${~settings.description}")

object Swiss:

  val maxPlayers           = 4000
  val maxForbiddenPairings = 1000
  val maxManualPairings    = 10_000

  opaque type Round = Int
  object Round extends OpaqueInt[Round]

  opaque type TieBreak = Double
  object TieBreak extends OpaqueDouble[TieBreak]

  opaque type Performance = Float
  object Performance extends OpaqueFloat[Performance]

  opaque type Score = Int
  object Score extends OpaqueInt[Score]

  case class IdName(_id: SwissId, name: String):
    inline def id = _id

  case class Settings(
      nbRounds: Int,
      rated: Boolean,
      description: Option[String] = None,
      position: Option[FEN],
      chatFor: ChatFor = ChatFor.default,
      password: Option[String] = None,
      conditions: SwissCondition.All,
      roundInterval: FiniteDuration,
      forbiddenPairings: String,
      manualPairings: String
  ):
    lazy val intervalSeconds = roundInterval.toSeconds.toInt
    def manualRounds         = intervalSeconds == Swiss.RoundInterval.manual
    def dailyInterval = (!manualRounds && intervalSeconds >= 24 * 3600) option intervalSeconds / 3600 / 24

  type ChatFor = Int
  object ChatFor:
    val NONE    = 0
    val LEADERS = 10
    val MEMBERS = 20
    val ALL     = 30
    val default = MEMBERS

  object RoundInterval:
    val auto   = -1
    val manual = 99999999

  def makeScore(points: SwissPoints, tieBreak: TieBreak, perf: Performance) =
    Score((points.value * 10000000 + tieBreak * 10000 + perf).toInt)

  def makeId = SwissId(lila.common.ThreadLocalRandom nextString 8)

  case class PastAndNext(past: List[Swiss], next: List[Swiss])

  case class RoundInfo(
      teamId: TeamId,
      chatFor: ChatFor
  )
