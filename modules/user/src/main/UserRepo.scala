package lila.user

import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats.toJSON
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import reactivemongo.api._
import reactivemongo.bson._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.rating.{ Glicko, Perf, PerfType }
import tube.userTube

object UserRepo extends UserRepo {
  protected def userTube = tube.userTube
}

trait UserRepo {

  protected implicit def userTube: lila.db.BsTubeInColl[User]

  import User.ID
  import User.{ BSONFields => F }

  val normalize = User normalize _

  def all: Fu[List[User]] = $find.all

  def topPerfSince(perf: String, since: DateTime, nb: Int): Fu[List[User]] =
    $find($query(topPerfSinceSelect(perf, since)) sort sortPerfDesc(perf), nb)

  def topPerfSinceSelect(perf: String, since: DateTime) =
    goodLadSelect ++ stablePerfSelect(perf) ++ perfSince(perf, since)

  def topNbGame(nb: Int): Fu[List[User]] =
    $find($query(enabledSelect) sort ($sort desc "count.game"), nb)

  def byId(id: ID): Fu[Option[User]] = $find byId id

  def byIds(ids: Iterable[ID]): Fu[List[User]] = $find byIds ids

  def byEmail(email: String): Fu[Option[User]] = $find one Json.obj(F.email -> email)

  def enabledByEmail(email: String): Fu[Option[User]] = byEmail(email) map (_ filter (_.enabled))

  def pair(x: Option[ID], y: Option[ID]): Fu[(Option[User], Option[User])] =
    $find byIds List(x, y).flatten map { users =>
      x.??(xx => users.find(_.id == xx)) ->
        y.??(yy => users.find(_.id == yy))
    }

  def byOrderedIds(ids: Iterable[ID]): Fu[List[User]] = $find byOrderedIds ids

  def enabledByIds(ids: Seq[ID]): Fu[List[User]] =
    $find(enabledSelect ++ $select.byIds(ids))

  def enabledById(id: ID): Fu[Option[User]] =
    $find.one(enabledSelect ++ $select.byId(id))

  def named(username: String): Fu[Option[User]] = $find byId normalize(username)

  def nameds(usernames: List[String]): Fu[List[User]] = $find byIds usernames.map(normalize)

  def byIdsSortRating(ids: Iterable[ID], nb: Int) =
    $find($query($select.byIds(ids) ++ goodLadSelect) sort sortPerfDesc(PerfType.Standard.key), nb)

  def allSortToints(nb: Int) = $find($query.all sort ($sort desc F.toints), nb)

  def usernameById(id: ID) = $primitive.one($select(id), F.username)(_.asOpt[String])

  def orderByGameCount(u1: String, u2: String): Fu[Option[(String, String)]] =
    userTube.coll.find(
      BSONDocument("_id" -> BSONDocument("$in" -> List(u1, u2))),
      BSONDocument(s"${F.count}.game" -> true)
    ).cursor[BSONDocument].collect[List]() map { docs =>
        docs.sortBy {
          _.getAs[BSONDocument](F.count) flatMap (_.getAs[Double]("game").map(_.toInt)) getOrElse 0
        }.map(_.getAs[String]("_id")).flatten match {
          case List(u1, u2) => (u1, u2).some
          case _            => none
        }
      }

  val lichessId = "lichess"
  def lichess = byId(lichessId)

  private type PerfLenses = List[(String, Perfs => Perf)]

  def setPerfs(user: User, perfs: Perfs, prev: Perfs) = {
    val lenses: PerfLenses = List(
      "standard" -> (_.standard),
      "chess960" -> (_.chess960),
      "kingOfTheHill" -> (_.kingOfTheHill),
      "threeCheck" -> (_.threeCheck),
      "bullet" -> (_.bullet),
      "blitz" -> (_.blitz),
      "classical" -> (_.classical),
      "correspondence" -> (_.correspondence),
      "puzzle" -> (_.puzzle))
    val diff = lenses.flatMap {
      case (name, lens) =>
        lens(perfs).nb != lens(prev).nb option {
          s"perfs.$name" -> Perf.perfBSONHandler.write(lens(perfs))
        }
    }
    $update($select(user.id), BSONDocument("$set" -> BSONDocument(diff)))
  }

  def setPerf(userId: String, perfName: String, perf: Perf) = $update($select(userId), $setBson(
    s"${F.perfs}.$perfName" -> Perf.perfBSONHandler.write(perf)
  ))

  def setProfile(id: ID, profile: Profile): Funit =
    $update($select(id), $setBson(F.profile -> Profile.tube.handler.write(profile)))

  def setTitle(id: ID, title: Option[String]): Funit = title match {
    case Some(t) => $update.field(id, F.title, t)
    case None    => $update($select(id), $unset(F.title))
  }

  def setPlayTime(u: User, playTime: User.PlayTime): Funit =
    $update($select(u.id), $setBson(F.playTime -> User.playTimeHandler.write(playTime)))

  val enabledSelect = Json.obj(F.enabled -> true)
  def engineSelect(v: Boolean) = Json.obj(F.engine -> v.fold(JsBoolean(true), $ne(true)))
  def stablePerfSelect(perf: String) = Json.obj(s"perfs.$perf.nb" -> $gte(30))
  val goodLadSelect = enabledSelect ++ engineSelect(false)
  def perfSince(perf: String, since: DateTime) = Json.obj(s"perfs.$perf.la" -> $gt($date(since)))
  val goodLadQuery = $query(goodLadSelect)

  def sortPerfDesc(perf: String) = $sort desc s"perfs.$perf.gl.r"
  val sortCreatedAtDesc = $sort desc F.createdAt

  def incNbGames(id: ID, rated: Boolean, ai: Boolean, result: Int, totalTime: Option[Int], tvTime: Option[Int]) = {
    val incs = List(
      "count.game".some,
      rated option "count.rated",
      ai option "count.ai",
      (result match {
        case -1 => "count.loss".some
        case 1  => "count.win".some
        case 0  => "count.draw".some
        case _  => none
      }),
      (result match {
        case -1 => "count.lossH".some
        case 1  => "count.winH".some
        case 0  => "count.drawH".some
        case _  => none
      }) ifFalse ai
    ).flatten.map(_ -> 1) ::: List(
        totalTime map (s"${F.playTime}.total" -> _),
        tvTime map (s"${F.playTime}.tv" -> _)
      ).flatten

    $update($select(id), $incBson(incs: _*))
  }

  def incToints(id: ID, nb: Int) = $update($select(id), $incBson("toints" -> nb))
  def removeAllToints = $update($select.all, $unset("toints"), multi = true)

  def authenticate(id: ID, password: String): Fu[Option[User]] =
    checkPassword(id, password) flatMap { _ ?? ($find byId id) }

  private case class AuthData(password: String, salt: String, enabled: Boolean, sha512: Boolean) {
    def compare(p: String) = password == sha512.fold(hash512(p, salt), hash(p, salt))
  }

  private object AuthData {

    import lila.db.JsTube.Helpers._
    import play.api.libs.json._

    private def defaults = Json.obj("sha512" -> false)

    lazy val reader = (__.json update merge(defaults)) andThen Json.reads[AuthData]
  }

  def checkPassword(id: ID, password: String): Fu[Boolean] =
    $projection.one($select(id), Seq("password", "salt", "enabled", "sha512")) { obj =>
      (AuthData.reader reads obj).asOpt
    } map {
      _ ?? (data => data.enabled && data.compare(password))
    }

  def getPasswordHash(id: ID): Fu[Option[String]] =
    $primitive.one($select(id), "password")(_.asOpt[String])

  def create(username: String, password: String, blind: Boolean): Fu[Option[User]] =
    !nameExists(username) flatMap {
      _ ?? {
        $insert.bson(newUser(username, password, blind)) >> named(normalize(username))
      }
    }

  def nameExists(username: String): Fu[Boolean] = idExists(normalize(username))
  def idExists(id: String): Fu[Boolean] = $count exists id

  def engineIds: Fu[Set[String]] = $primitive(Json.obj("engine" -> true), "_id")(_.asOpt[String]) map (_.toSet)

  def usernamesLike(username: String, max: Int = 10): Fu[List[String]] = {
    import java.util.regex.Matcher.quoteReplacement
    val escaped = """^([\w-]*).*$""".r.replaceAllIn(normalize(username), m => quoteReplacement(m group 1))
    val regex = "^" + escaped + ".*$"
    $primitive(
      $select.byId($regex(regex)) ++ enabledSelect,
      F.username,
      _ sort $sort.desc("_id"),
      max.some
    )(_.asOpt[String])
  }

  def toggleEngine(id: ID): Funit = $update.docBson[ID, User](id) { u =>
    $setBson("engine" -> BSONBoolean(!u.engine))
  }

  def toggleIpBan(id: ID) = $update.doc[ID, User](id) { u => $set("ipBan" -> !u.ipBan) }

  def updateTroll(user: User) = $update.field(user.id, "troll", user.troll)

  def isEngine(id: ID): Fu[Boolean] = $count.exists($select(id) ++ engineSelect(true))

  def setRoles(id: ID, roles: List[String]) = $update.field(id, "roles", roles)

  def enable(id: ID) = $update.field(id, "enabled", true)

  def disable(id: ID) = $update.field(id, "enabled", false)

  def passwd(id: ID, password: String): Funit =
    $primitive.one($select(id), "salt")(_.asOpt[String]) flatMap { saltOption =>
      saltOption ?? { salt =>
        $update($select(id), $set(Json.obj(
          "password" -> hash(password, salt),
          "sha512" -> false)))
      }
    }

  def email(id: ID, email: String): Funit = $update.field(id, "email", email)

  def setSeenAt(id: ID) {
    $update.fieldUnchecked(id, "seenAt", $date(DateTime.now))
  }

  def recentlySeenIds(since: DateTime) =
    $primitive(enabledSelect ++ Json.obj(
      "seenAt" -> $gt($date(since)),
      "count.game" -> $gt(4)
    ), "_id")(_.asOpt[String])

  def setLang(id: ID, lang: String) {
    $update.fieldUnchecked(id, "lang", lang)
  }

  def idsSumToints(ids: Iterable[String]): Fu[Int] = ids.isEmpty ? fuccess(0) | {
    import reactivemongo.core.commands._
    val command = Aggregate(userTube.coll.name, Seq(
      Match(BSONDocument("_id" -> BSONDocument("$in" -> ids))),
      Group(BSONBoolean(true))(F.toints -> SumField(F.toints))
    ))
    userTube.coll.db.command(command) map { stream =>
      stream.toList.headOption flatMap { obj =>
        toJSON(obj).asOpt[JsObject]
      } flatMap { _ int F.toints }
    } map (~_)
  }

  def filterByEngine(userIds: List[String]): Fu[List[String]] =
    $primitive(Json.obj("_id" -> $in(userIds)) ++ engineSelect(true), F.username)(_.asOpt[String])

  private def newUser(username: String, password: String, blind: Boolean) = {

    val salt = ornicar.scalalib.Random nextStringUppercase 32
    implicit def countHandler = Count.tube.handler
    implicit def perfsHandler = Perfs.tube.handler
    import lila.db.BSON.BSONJodaDateTimeHandler

    BSONDocument(
      F.id -> normalize(username),
      F.username -> username,
      "password" -> hash(password, salt),
      "salt" -> salt,
      F.perfs -> Json.obj(),
      F.count -> Count.default,
      F.enabled -> true,
      F.createdAt -> DateTime.now,
      F.seenAt -> DateTime.now) ++ {
        if (blind) BSONDocument("blind" -> true) else BSONDocument()
      }
  }

  private def hash(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha1
  private def hash512(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha512
}
