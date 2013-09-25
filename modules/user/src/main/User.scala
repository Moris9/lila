package lila.user

import chess.Speed
import org.joda.time.DateTime
import scala.concurrent.duration._

case class User(
    id: String,
    username: String,
    elo: Int,
    speedElos: SpeedElos,
    variantElos: VariantElos,
    count: Count,
    troll: Boolean = false,
    ipBan: Boolean = false,
    enabled: Boolean,
    roles: List[String],
    settings: Map[String, String] = Map.empty,
    bio: Option[String] = None,
    engine: Boolean = false,
    toints: Int = 0,
    createdAt: DateTime,
    seenAt: Option[DateTime],
    lang: Option[String]) extends Ordered[User] {

  override def equals(other: Any) = other match {
    case u: User ⇒ id == u.id
    case _       ⇒ false
  }

  def compare(other: User) = id compare other.id

  def noTroll = !troll

  def canTeam = true

  def disabled = !enabled

  def usernameWithElo = "%s (%d)".format(username, elo)

  def setting(name: String): Option[Any] = settings get name

  def nonEmptyBio = bio filter ("" !=)

  def hasGames = count.game > 0

  def countRated = count.rated

  private val recentDuration = 10.minutes
  def seenRecently: Boolean = timeNoSee < recentDuration

  def timeNoSee: Duration = seenAt.fold[Duration](Duration.Inf) { s ⇒
    (nowMillis - s.getMillis).millis
  }
}

object User {

  type ID = String

  val STARTING_ELO = 1200

  val anonymous = "Anonymous"

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def countTube = Count.tube
  private implicit def speedElosTube = SpeedElos.tube
  private implicit def variantElosTube = VariantElos.tube

  private[user] lazy val tube = Tube[User](
    (__.json update (
      merge(defaults) andThen readDate('createdAt) andThen readDateOpt('seenAt)
    )) andThen Json.reads[User],
    Json.writes[User] andThen (__.json update writeDate('createdAt)) andThen (__.json update writeDateOpt('seenAt))
  )

  def normalize(username: String) = username.toLowerCase

  private def defaults = Json.obj(
    "speedElos" -> SpeedElos.default,
    "variantElos" -> VariantElos.default,
    "troll" -> false,
    "ipBan" -> false,
    "settings" -> Json.obj(),
    "engine" -> false,
    "toints" -> 0,
    "roles" -> Json.arr(),
    "seenAt" -> none[DateTime],
    "lang" -> none[String])
}
