package lila.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import reactivemongo.api.bson.Macros
import reactivemongo.api.ReadPreference
import scala.util.Random

import lila.common.{ ApiVersion, HTTPRequest, IpAddress }
import lila.db.dsl._
import lila.user.User

final class Store(val coll: Coll, localIp: IpAddress) {

  import Store._

  def save(
      sessionId: String,
      userId: User.ID,
      req: RequestHeader,
      apiVersion: Option[ApiVersion],
      up: Boolean,
      fp: Option[FingerPrint]
  ): Funit =
    coll.insert
      .one(
        $doc(
          "_id"  -> sessionId,
          "user" -> userId,
          "ip" -> (HTTPRequest.lastRemoteAddress(req) match {
            // randomize stresser IPs to relieve mod tools
            case ip if ip == localIp => IpAddress(s"127.0.${Random nextInt 256}.${Random nextInt 256}")
            case ip                  => ip
          }),
          "ua"   -> HTTPRequest.userAgent(req).|("?"),
          "date" -> DateTime.now,
          "up"   -> up,
          "api"  -> apiVersion.map(_.value),
          "fp"   -> fp.flatMap(FingerHash.apply).flatMap(fingerHashBSONHandler.writeOpt)
        )
      )
      .void

  private val userIdFingerprintProjection = $doc(
    "user" -> true,
    "fp"   -> true,
    "date" -> true,
    "_id"  -> false
  )

  def userId(sessionId: String): Fu[Option[User.ID]] =
    coll.primitiveOne[User.ID]($doc("_id" -> sessionId, "up" -> true), "user")

  case class UserIdAndFingerprint(user: User.ID, fp: Option[FingerHash], date: DateTime) {
    def isOld = date isBefore DateTime.now.minusHours(12)
  }
  implicit private val UserIdAndFingerprintBSONReader = Macros.reader[UserIdAndFingerprint]

  def userIdAndFingerprint(sessionId: String): Fu[Option[UserIdAndFingerprint]] =
    coll
      .find(
        $doc("_id" -> sessionId, "up" -> true),
        userIdFingerprintProjection.some
      )
      .one[UserIdAndFingerprint]

  def setDateToNow(sessionId: String): Unit =
    coll.updateFieldUnchecked($id(sessionId), "date", DateTime.now)

  def delete(sessionId: String): Funit =
    coll.update
      .one(
        $id(sessionId),
        $set("up" -> false)
      )
      .void

  def closeUserAndSessionId(userId: User.ID, sessionId: String): Funit =
    coll.update
      .one(
        $doc("user" -> userId, "_id" -> sessionId, "up" -> true),
        $set("up"   -> false)
      )
      .void

  def closeUserExceptSessionId(userId: User.ID, sessionId: String): Funit =
    coll.update
      .one(
        $doc("user" -> userId, "_id" -> $ne(sessionId), "up" -> true),
        $set("up"   -> false),
        multi = true
      )
      .void

  // useful when closing an account,
  // we want to logout too
  def disconnect(userId: User.ID): Funit =
    coll.update
      .one(
        $doc("user" -> userId),
        $set("up"   -> false),
        multi = true
      )
      .void

  implicit private val UserSessionBSONHandler = Macros.handler[UserSession]
  def openSessions(userId: User.ID, nb: Int): Fu[List[UserSession]] =
    coll.ext
      .find(
        $doc("user" -> userId, "up" -> true)
      )
      .sort($doc("date" -> -1))
      .cursor[UserSession]()
      .gather[List](nb)

  def setFingerPrint(id: String, fp: FingerPrint): Fu[FingerHash] =
    FingerHash(fp) match {
      case None       => fufail(s"Can't hash $id's fingerprint $fp")
      case Some(hash) => coll.updateField($doc("_id" -> id), "fp", hash) inject hash
    }

  def chronoInfoByUser(userId: User.ID): Fu[List[Info]] =
    coll.ext
      .find(
        $doc(
          "user" -> userId,
          "date" $gt DateTime.now.minusYears(2)
        ),
        $doc("_id" -> false, "ip" -> true, "ua" -> true, "fp" -> true, "date" -> true)
      )
      .sort($sort desc "date")
      .list[Info]()(InfoReader)

  private case class DedupInfo(_id: String, ip: String, ua: String) {
    def compositeKey = s"$ip $ua"
  }
  implicit private val DedupInfoReader = Macros.reader[DedupInfo]

  def dedup(userId: User.ID, keepSessionId: String): Funit =
    coll.ext
      .find(
        $doc(
          "user" -> userId,
          "up"   -> true
        )
      )
      .sort($doc("date" -> -1))
      .cursor[DedupInfo]()
      .gather[List]() flatMap { sessions =>
      val olds = sessions
        .groupBy(_.compositeKey)
        .values
        .map(_ drop 1)
        .flatten
        .filter(_._id != keepSessionId)
      coll.delete.one($inIds(olds.map(_._id))).void
    }

  implicit private val IpAndFpReader = Macros.reader[IpAndFp]

  def ipsAndFps(userIds: List[User.ID], max: Int = 100): Fu[List[IpAndFp]] =
    coll.ext.find($doc("user" $in userIds)).list[IpAndFp](max, ReadPreference.secondaryPreferred)

  private[security] def recentByIpExists(ip: IpAddress): Fu[Boolean] =
    coll.secondaryPreferred.exists(
      $doc("ip" -> ip, "date" -> $gt(DateTime.now minusDays 7))
    )

  private[security] def recentByPrintExists(fp: FingerPrint): Fu[Boolean] =
    FingerHash(fp) ?? { hash =>
      coll.secondaryPreferred.exists(
        $doc("fp" -> hash, "date" -> $gt(DateTime.now minusDays 7))
      )
    }
}

object Store {

  case class Dated[V](value: V, date: DateTime) extends Ordered[Dated[V]] {
    def compare(other: Dated[V]) = other.date compareTo date
  }

  case class Info(ip: IpAddress, ua: String, fp: Option[FingerHash], date: DateTime) {
    def datedIp = Dated(ip, date)
    def datedFp = fp.map { Dated(_, date) }
    def datedUa = Dated(ua, date)
  }

  implicit val fingerHashBSONHandler = stringIsoHandler[FingerHash]
  implicit val InfoReader            = Macros.reader[Info]
}
