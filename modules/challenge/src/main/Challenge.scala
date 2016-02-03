package lila.challenge

import chess.variant.Variant
import chess.{ Mode, Clock, Speed }
import org.joda.time.DateTime

import lila.game.PerfPicker
import lila.rating.PerfType
import lila.user.User

case class Challenge(
    _id: String,
    status: Challenge.Status,
    variant: Variant,
    initialFen: Option[String],
    timeControl: Challenge.TimeControl,
    mode: Mode,
    color: Challenge.ColorChoice,
    challenger: EitherChallenger,
    destUser: Option[Challenge.Registered],
    createdAt: DateTime,
    seenAt: DateTime,
    expiresAt: DateTime) {

  import Challenge._

  def id = _id

  def challengerUser = challenger.right.toOption
  def challengerUserId = challengerUser.map(_.id)
  def destUserId = destUser.map(_.id)

  def daysPerTurn = timeControl match {
    case TimeControl.Correspondence(d) => d.some
    case _                             => none
  }

  def clock = timeControl match {
    case c: TimeControl.Clock => c.some
    case _                    => none
  }

  def openDest = destUser.isEmpty
  def active = status == Status.Created || status == Status.Offline
  def declined = status == Status.Declined
  def accepted = status == Status.Accepted

  lazy val chessColor = ColorChoice toChess color

  lazy val perfType = perfTypeOf(variant, timeControl)
}

object Challenge {

  type ID = String

  sealed abstract class Status(val id: Int)
  object Status {
    case object Created extends Status(10)
    case object Offline extends Status(15)
    case object Canceled extends Status(20)
    case object Declined extends Status(30)
    case object Accepted extends Status(40)
    val all = List(Created, Offline, Canceled, Declined, Accepted)
    def apply(id: Int): Option[Status] = all.find(_.id == id)
  }

  case class Rating(int: Int, provisional: Boolean)
  object Rating {
    def apply(p: lila.rating.Perf): Rating = Rating(p.intRating, p.provisional)
  }

  case class Registered(id: User.ID, rating: Rating)
  case class Anonymous(secret: String)

  sealed trait TimeControl
  object TimeControl {
    case object Unlimited extends TimeControl
    case class Correspondence(days: Int) extends TimeControl
    case class Clock(limit: Int, increment: Int) extends TimeControl {
      // All durations are expressed in seconds
      def show = chessClock.show
      lazy val chessClock = chess.Clock(limit, increment)
    }
  }

  sealed trait ColorChoice
  object ColorChoice {
    case object Random extends ColorChoice
    case object White extends ColorChoice
    case object Black extends ColorChoice
    def toChess(c: ColorChoice): chess.Color = c match {
      case Random => chess.Color(scala.util.Random.nextBoolean)
      case White  => chess.White
      case Black  => chess.Black
    }
  }

  private def speedOf(timeControl: TimeControl) = timeControl match {
    case c: TimeControl.Clock => Speed(c.chessClock)
    case _                    => Speed.Correspondence
  }

  private def perfTypeOf(variant: Variant, timeControl: TimeControl) =
    PerfPicker.perfType(speedOf(timeControl), variant, timeControl match {
      case TimeControl.Correspondence(d) => d.some
      case _                             => none
    }) | PerfType.Correspondence

  private val idSize = 8

  private def randomId = ornicar.scalalib.Random nextStringUppercase idSize

  private def toRegistered(variant: Variant, timeControl: TimeControl)(u: User) =
    Registered(u.id, Rating(u.perfs(perfTypeOf(variant, timeControl))))

  def make(
    variant: Variant,
    initialFen: Option[String],
    timeControl: TimeControl,
    mode: Mode,
    color: String,
    challenger: Option[User],
    destUser: Option[User]): Challenge = new Challenge(
    _id = randomId,
    status = Status.Created,
    variant = variant,
    initialFen = initialFen.ifTrue(variant == chess.variant.FromPosition),
    timeControl = timeControl,
    mode = mode,
    color = color match {
      case "white" => ColorChoice.White
      case "black" => ColorChoice.Black
      case _       => ColorChoice.Random
    },
    challenger = challenger.fold[EitherChallenger](Left(Anonymous(randomId))) { u =>
      Right(toRegistered(variant, timeControl)(u))
    },
    destUser = destUser map toRegistered(variant, timeControl),
    createdAt = DateTime.now,
    seenAt = DateTime.now,
    expiresAt = inTwoWeeks)
}
