package lila.pref

import play.api.libs.json._

import lila.db.JsTube
import lila.db.JsTube.Helpers._
import lila.user.User

case class Pref(
    _id: String, // user id
    dark: Boolean,
    is3d: Boolean,
    theme: String,
    pieceSet: String,
    theme3d: String,
    pieceSet3d: String,
    blindfold: Int,
    autoQueen: Int,
    autoThreefold: Int,
    takeback: Int,
    clockTenths: Boolean,
    clockBar: Boolean,
    clockSound: Boolean,
    premove: Boolean,
    animation: Int,
    captured: Boolean,
    follow: Boolean,
    highlight: Boolean,
    destination: Boolean,
    coords: Int,
    replay: Int,
    challenge: Int,
    coordColor: Int,
    puzzleDifficulty: Int,
    submitMove: Int,
    tags: Map[String, String] = Map.empty) {

  import Pref._

  def id = _id

  def realTheme = Theme(theme)
  def realPieceSet = PieceSet(pieceSet)
  def realTheme3d = Theme3d(theme3d)
  def realPieceSet3d = PieceSet3d(pieceSet3d)

  def coordColorName = Color.choices.toMap.get(coordColor).fold("random")(_.toLowerCase)

  def hasSeenVerifyTitle = tags contains Tag.verifyTitle

  def get(name: String): Option[String] = name match {
    case "bg"         => dark.fold("dark", "light").some
    case "theme"      => theme.some
    case "pieceSet"   => pieceSet.some
    case "theme3d"    => theme3d.some
    case "pieceSet3d" => pieceSet3d.some
    case "is3d"       => is3d.toString.some
    case _            => none
  }
  def set(name: String, value: String): Option[Pref] = name match {
    case "bg"         => Pref.bgs get value map { b => copy(dark = b) }
    case "theme"      => Theme.allByName get value map { t => copy(theme = t.name) }
    case "pieceSet"   => PieceSet.allByName get value map { p => copy(pieceSet = p.name) }
    case "theme3d"    => Theme3d.allByName get value map { t => copy(theme3d = t.name) }
    case "pieceSet3d" => PieceSet3d.allByName get value map { p => copy(pieceSet3d = p.name) }
    case "is3d"       => copy(is3d = value == "true").some
    case _            => none
  }

  def animationFactor = animation match {
    case Animation.NONE   => 0
    case Animation.FAST   => 0.5f
    case Animation.NORMAL => 1
    case Animation.SLOW   => 2
    case _                => 1
  }

  def isBlindfold = blindfold == Pref.Blindfold.YES
}

object Pref {

  object Tag {
    val verifyTitle = "verifyTitle"
  }

  object Difficulty {
    val EASY = 1
    val NORMAL = 2
    val HARD = 3

    val choices = Seq(
      EASY -> "Easy",
      NORMAL -> "Normal",
      HARD -> "Hard")
  }

  object Color {
    val WHITE = 1
    val RANDOM = 2
    val BLACK = 3

    val choices = Seq(
      WHITE -> "White",
      RANDOM -> "Random",
      BLACK -> "Black")
  }

  object AutoQueen {
    val NEVER = 1
    val PREMOVE = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      PREMOVE -> "When premoving")
  }

  object SubmitMove {
    val NEVER = 0
    val CORRESPONDENCE = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER -> "Never",
      CORRESPONDENCE -> "On correspondence games",
      ALWAYS -> "Always")
  }

  object Blindfold {
    val NO = 0
    val YES = 1

    val choices = Seq(
      NO -> "What? No!",
      YES -> "Yes, hide the pieces")
  }

  object AutoThreefold {
    val NEVER = 1
    val TIME = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      TIME -> "When time remaining < 30 seconds")
  }

  object Takeback {
    val NEVER = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      CASUAL -> "In casual games only")
  }

  object Animation {
    val NONE = 0
    val FAST = 1
    val NORMAL = 2
    val SLOW = 3

    val choices = Seq(
      NONE -> "None",
      FAST -> "Fast",
      NORMAL -> "Normal",
      SLOW -> "Slow")
  }

  object Coords {
    val NONE = 0
    val INSIDE = 1
    val OUTSIDE = 2

    val choices = Seq(
      NONE -> "No",
      INSIDE -> "Inside the board",
      OUTSIDE -> "Outside the board")
  }

  object Replay {
    val NEVER = 0
    val SLOW = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER -> "Never",
      SLOW -> "On slow games",
      ALWAYS -> "Always")
  }

  object Challenge {
    val NEVER = 1
    val RATING = 2
    val FRIEND = 3
    val ALWAYS = 4

    private val ratingThreshold = 500

    val choices = Seq(
      NEVER -> "Never",
      RATING -> s"If rating is ± $ratingThreshold",
      FRIEND -> "Only friends",
      ALWAYS -> "Always")

    def block(from: User, to: User, pref: Int, follow: Boolean): Option[String] = pref match {
      case NEVER => "{{user}} doesn't accept challenges.".some
      case RATING if math.abs(from.perfs.bestRating - to.perfs.bestRating) > ratingThreshold =>
        s"{{user}} only accepts challenges if rating is ± $ratingThreshold.".some
      case FRIEND if !follow => "{{user}} only accepts challenges from friends.".some
      case _                 => none
    }
  }

  def create(id: String) = default.copy(_id = id)

  lazy val default = Pref(
    _id = "",
    dark = false,
    is3d = false,
    theme = Theme.default.name,
    pieceSet = PieceSet.default.name,
    theme3d = Theme3d.default.name,
    pieceSet3d = PieceSet3d.default.name,
    blindfold = Blindfold.NO,
    autoQueen = AutoQueen.PREMOVE,
    autoThreefold = AutoThreefold.TIME,
    takeback = Takeback.ALWAYS,
    clockTenths = true,
    clockBar = true,
    clockSound = true,
    premove = true,
    animation = 2,
    captured = true,
    follow = true,
    highlight = true,
    destination = true,
    coords = Coords.OUTSIDE,
    replay = Replay.ALWAYS,
    challenge = Challenge.RATING,
    coordColor = Color.RANDOM,
    puzzleDifficulty = Difficulty.NORMAL,
    submitMove = SubmitMove.CORRESPONDENCE,
    tags = Map.empty)

  import ornicar.scalalib.Zero
  implicit def PrefZero: Zero[Pref] = Zero.instance(default)

  private val bgs = Map("light" -> false, "dark" -> true)
}
