package lila.pref

import play.api.libs.json.{ Json, OWrites }
import reactivemongo.api.bson.*
import NotificationPref.*
import alleycats.Zero

// #TODO opaque type
case class Allows(value: Int) extends AnyVal with IntValue:
  def push: Boolean   = (value & NotificationPref.PUSH) != 0
  def web: Boolean    = (value & NotificationPref.WEB) != 0
  def device: Boolean = (value & NotificationPref.DEVICE) != 0
  def bell: Boolean   = (value & NotificationPref.BELL) != 0
  def any: Boolean    = value != 0

object Allows:
  given Zero[Allows] = Zero(Allows(0))

  val canFilter = Set(
    "privateMessage",
    "challenge",
    "mention",
    "streamStart",
    "tournamentSoon",
    "gameEvent",
    "invitedStudy"
  )

  def fromForm(bell: Boolean, push: Boolean): Allows =
    Allows((bell ?? BELL) | (push ?? PUSH))

  def toForm(allows: Allows): Some[(Boolean, Boolean)] =
    Some((allows.bell, allows.push))

  def fromCode(code: Int) = Allows(code)

case class NotifyAllows(userId: UserId, allows: Allows):
  export allows.*

// take care with NotificationPref field names - they map directly to db and ws channels

case class NotificationPref(
    privateMessage: Allows,
    challenge: Allows,
    mention: Allows,
    streamStart: Allows,
    tournamentSoon: Allows,
    gameEvent: Allows,
    invitedStudy: Allows,
    correspondenceEmail: Int
):
  def allows(key: String): Allows =
    NotificationPref.Event.byKey.get(key) ?? allows

  def allows(event: Event): Allows = event match
    case PrivateMessage => privateMessage
    case Challenge      => challenge
    case Mention        => mention
    case InvitedStudy   => invitedStudy
    case StreamStart    => streamStart
    case TournamentSoon => tournamentSoon
    case GameEvent      => gameEvent

object NotificationPref:
  val BELL   = 1
  val WEB    = 2
  val DEVICE = 4
  val PUSH   = WEB | DEVICE

  enum Event:
    case PrivateMessage
    case Challenge
    case Mention
    case InvitedStudy
    case StreamStart
    case TournamentSoon
    case GameEvent

    def key = lila.common.String.lcfirst(getClass.getSimpleName)

  object Event:
    val byKey = values.map { v => v.key -> v }.toMap

  export Event.*

  lazy val default: NotificationPref = NotificationPref(
    privateMessage = Allows(BELL | PUSH),
    challenge = Allows(BELL | PUSH),
    invitedStudy = Allows(BELL | PUSH),
    mention = Allows(BELL | PUSH),
    streamStart = Allows(BELL | PUSH),
    tournamentSoon = Allows(PUSH),
    gameEvent = Allows(PUSH),
    correspondenceEmail = 0
  )

  // implicit private val AllowsBSONHandler =
  //  lila.db.dsl.intAnyValHandler[Allows](_.value, Allows.apply)
  given BSONHandler[Allows] =
    lila.db.dsl.intAnyValHandler[Allows](_.value, Allows.apply)

  given BSONDocumentHandler[NotificationPref] = Macros.handler

  given notificationDataJsonWriter: OWrites[NotificationPref] =
    OWrites[NotificationPref] { data =>
      Json.obj(
        "privateMessage"      -> allowsToJson(data.privateMessage),
        "mention"             -> allowsToJson(data.mention),
        "streamStart"         -> allowsToJson(data.streamStart),
        "challenge"           -> allowsToJson(data.challenge),
        "tournamentSoon"      -> allowsToJson(data.tournamentSoon),
        "gameEvent"           -> allowsToJson(data.gameEvent),
        "invitedStudy"        -> allowsToJson(data.invitedStudy),
        "correspondenceEmail" -> (data.correspondenceEmail != 0)
      )
    }

  private def allowsToJson(v: Allows) = List(
    Map(BELL -> "bell", PUSH -> "push") collect {
      case (tpe, str) if (v.value & tpe) != 0 => str
    }
  )
