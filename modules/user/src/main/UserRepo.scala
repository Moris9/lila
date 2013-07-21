package lila.user

import com.roundeights.hasher.Implicits._
import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats.toJSON
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import reactivemongo.api._
import tube.userTube

object UserRepo {

  type ID = String

  val normalize = User normalize _

  def all: Fu[List[User]] = $find.all

  def byId(id: ID): Fu[Option[User]] = $find byId id

  def byIds(ids: Iterable[ID]): Fu[List[User]] = $find byIds ids

  def byOrderedIds(ids: Iterable[ID]): Fu[List[User]] = $find byOrderedIds ids

  def enabledByIds(ids: Seq[ID]): Fu[List[User]] =
    $find(enabledQuery ++ $select.byIds(ids))

  def named(username: String): Fu[Option[User]] = $find byId normalize(username)

  def nameds(usernames: List[String]): Fu[List[User]] = $find byIds usernames.map(normalize)

  def byIdsSortElo(ids: Seq[ID], max: Int) = $find($query byIds ids sort sortEloDesc, max)

  def allSortToints(nb: Int) = $find($query.all sort ("toints" -> $sort.desc), nb)

  def usernameById(id: ID) = $primitive.one($select(id), "username")(_.asOpt[String])

  def rank(user: User) = $count(enabledQuery ++ Json.obj("elo" -> $gt(user.elo))) map (1+)

  def setElo(id: ID, elo: Int, speed: String, se: SpeedElo): Funit = $update($select(id), $set(
    "elo" -> elo,
    "speedElos.%s.nb".format(speed) -> se.nb,
    "speedElos.%s.elo".format(speed) -> se.elo
  ))

  def setEloOnly(id: ID, elo: Int): Funit = $update($select(id), $set("elo" -> elo))

  val enabledQuery = Json.obj("enabled" -> true)

  val sortEloDesc = $sort desc "elo"

  def incNbGames(id: ID, rated: Boolean, ai: Boolean, result: Option[Int]) = {
    val incs = List(
      "count.game".some,
      "count.rated".some filter (_ ⇒ rated),
      "count.ai".some filter (_ ⇒ ai),
      (result match {
        case Some(-1) ⇒ "count.loss".some
        case Some(1)  ⇒ "count.win".some
        case Some(0)  ⇒ "count.draw".some
        case _        ⇒ none
      }),
      (result match {
        case Some(-1) ⇒ "count.lossH".some
        case Some(1)  ⇒ "count.winH".some
        case Some(0)  ⇒ "count.drawH".some
        case _        ⇒ none
      }) filterNot (_ ⇒ ai)
    ).flatten map (_ -> 1)

    $update($select(id), $inc(incs: _*))
  }

  def incToints(id: ID)(nb: Int) = $update($select(id), $inc("toints" -> nb))

  def averageElo: Fu[Float] = $primitive($select.all, "elo")(_.asOpt[Float]) map { elos ⇒
    elos.sum / elos.size.toFloat
  }

  def saveSetting(id: ID, key: String, value: String): Funit =
    $update($select(id), $set(("settings." + key) -> value))

  def getSetting(id: ID, key: String): Fu[Option[String]] =
    $primitive.one($select(id), "settings") {
      _.asOpt[Map[String, String]] flatMap (_ get key)
    }

  def authenticate(id: ID, password: String): Fu[Option[User]] =
    checkPassword(id, password) flatMap { _ ?? ($find byId id) }

  private case class AuthData(password: String, salt: String, enabled: Boolean, sha512: Boolean) {
    def compare(p: String) = password == sha512.fold(hash512(p, salt), hash(p, salt))
  }

  private object AuthData {

    import lila.db.Tube.Helpers._
    import play.api.libs.json._

    private def defaults = Json.obj("sha512" -> false)

    lazy val reader = (__.json update merge(defaults)) andThen Json.reads[AuthData]
  }

  def checkPassword(id: ID, password: String): Fu[Boolean] =
    $projection.one($select(id), Seq("password", "salt", "enabled", "sha512")) { obj ⇒
      (AuthData.reader reads obj).asOpt
    } map {
      _ ?? (data ⇒ data.enabled && data.compare(password))
    }

  def create(username: String, password: String): Fu[Option[User]] =
    !nameExists(username) flatMap {
      _ ?? {
        $insert(newUser(username, password)) >> named(normalize(username))
      }
    }

  def nameExists(username: String): Fu[Boolean] = $count exists normalize(username)

  def usernamesLike(username: String, max: Int = 10): Fu[List[String]] = {
    import java.util.regex.Matcher.quoteReplacement
    val escaped = """^([\w-]*).*$""".r.replaceAllIn(normalize(username), m ⇒ quoteReplacement(m group 1))
    val regex = "^" + escaped + ".*$"
    $primitive(
      $select byId $regex(regex),
      "username",
      _ sort ("_id" -> $sort.desc),
      max.some
    )(_.asOpt[String])
  }

  def toggleEngine(id: ID): Funit = $update.doc[ID, User](id) { u ⇒ $set("engine" -> !u.engine) }

  def toggleIpBan(id: ID) = $update.doc[ID, User](id) { u ⇒ $set("ipBan" -> !u.ipBan) }

  def updateTroll(user: User) = $update.field(user.id, "troll", user.troll)

  def isEngine(id: ID): Fu[Boolean] = $count.exists($select(id) ++ Json.obj("engine" -> true))

  def setRoles(id: ID, roles: List[String]) = $update.field(id, "roles", roles)

  def setBio(id: ID, bio: String) = $update.field(id, "bio", bio)

  def setSpeedElos(id: ID)(ses: SpeedElos) = {
    import tube.speedElosTube
    $update.field(id, "speedElos", ses)
  }

  def enable(id: ID) = $update.field(id, "enabled", true)

  def disable(id: ID) = $update.field(id, "enabled", false)

  def passwd(id: ID, password: String): Funit =
    $primitive.one($select(id), "salt")(_.asOpt[String]) flatMap { saltOption ⇒
      saltOption ?? { salt ⇒
        $update($select(id), $set(Json.obj(
          "password" -> hash(password, salt),
          "sha512" -> false)))
      }
    }

  def setSeenAt(id: ID) {
    $update.fieldUnchecked(id, "seenAt", $date(DateTime.now))
  }

  def idsAverageElo(ids: Iterable[String]): Fu[Int] = ids.isEmpty ? fuccess(0) | {
    import reactivemongo.bson._
    import reactivemongo.core.commands._
    val command = Aggregate(userTube.coll.name, Seq(
      Match(BSONDocument("_id" -> BSONDocument("$in" -> ids))),
      Group(BSONBoolean(true))("elo" -> SumField("elo"))
    ))
    userTube.coll.db.command(command) map { stream ⇒
      stream.toList.headOption flatMap { obj ⇒
        toJSON(obj).asOpt[JsObject]
      } flatMap { _ int "elo" }
    } map (~_ / ids.size)
  }

  def idsSumToints(ids: Iterable[String]): Fu[Int] = ids.isEmpty ? fuccess(0) | {
    import reactivemongo.bson._
    import reactivemongo.core.commands._
    val command = Aggregate(userTube.coll.name, Seq(
      Match(BSONDocument("_id" -> BSONDocument("$in" -> ids))),
      Group(BSONBoolean(true))("toints" -> SumField("toints"))
    ))
    userTube.coll.db.command(command) map { stream ⇒
      stream.toList.headOption flatMap { obj ⇒
        toJSON(obj).asOpt[JsObject]
      } flatMap { _ int "toints" }
    } map (~_)
  }

  private def newUser(username: String, password: String) = {

    val salt = Random nextString 32 
    implicit def speedElosTube = SpeedElos.tube
    implicit def countTube = Count.tube

    Json.obj(
      "_id" -> normalize(username),
      "username" -> username,
      "password" -> hash(password, salt),
      "salt" -> salt,
      "elo" -> User.STARTING_ELO,
      "count" -> Count.default,
      "enabled" -> true,
      "createdAt" -> $date(DateTime.now),
      "seenAt" -> $date(DateTime.now))
  }

  private def hash(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha1
  private def hash512(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha512
}
