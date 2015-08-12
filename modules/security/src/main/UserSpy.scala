package lila.security

import scala.concurrent.Future

import play.api.mvc.RequestHeader
import reactivemongo.bson._

import lila.common.PimpedJson._
import lila.db.api._
import lila.user.{ User, UserRepo }
import tube.storeColl

case class UserSpy(
    ips: List[UserSpy.IPData],
    uas: List[String],
    usersSharingIp: List[User],
    usersSharingFingerprint: List[User]) {

  import UserSpy.OtherUser

  def ipStrings = ips map (_.ip)

  def ipsByLocations: List[(Location, List[UserSpy.IPData])] =
    ips.sortBy(_.ip).groupBy(_.location).toList.sortBy(_._1.comparable)

  lazy val otherUsers: List[OtherUser] = {
    usersSharingIp.map { u =>
      OtherUser(u, true, usersSharingFingerprint contains u)
    } ::: usersSharingFingerprint.filterNot(usersSharingIp.contains).map {
      OtherUser(_, false, true)
    }
  }.sortBy(-_.user.createdAt.getMillis)
}

object UserSpy {

  case class OtherUser(user: User, byIp: Boolean, byFingerprint: Boolean)

  type IP = String
  type Fingerprint = String
  type Value = String

  case class IPData(ip: IP, blocked: Boolean, location: Location, tor: Boolean)

  private[security] def apply(firewall: Firewall, geoIP: GeoIP)(userId: String): Fu[UserSpy] = for {
    user ← UserRepo named userId flatten "[spy] user not found"
    infos ← Store.findInfoByUser(user.id)
    ips = infos.map(_.ip).distinct
    blockedIps ← (ips map firewall.blocksIp).sequenceFu
    tors = ips.map { ip =>
      infos.exists { x => x.ip == ip && x.isTorExitNode }
    }
    locations <- scala.concurrent.Future {
      ips zip tors map {
        case (_, true) => Location.tor
        case (ip, _)   => geoIP orUnknown ip
      }
    }
    sharingIp ← exploreSimilar("ip")(user)
    sharingFingerprint ← exploreSimilar("fp")(user)
  } yield UserSpy(
    ips = ips zip blockedIps zip locations zip tors map {
      case (((ip, blocked), location), tor) => IPData(ip, blocked, location, tor)
    },
    uas = infos.map(_.ua).distinct,
    usersSharingIp = (sharingIp + user).toList.sortBy(-_.createdAt.getMillis),
    usersSharingFingerprint = (sharingFingerprint + user).toList.sortBy(-_.createdAt.getMillis))

  private def exploreSimilar(field: String)(user: User): Fu[Set[User]] =
    nextValues(field)(user) flatMap { nValues =>
      nextUsers(field)(nValues, user) map { _ + user }
    }

  private def nextValues(field: String)(user: User): Fu[Set[Value]] =
    storeColl.find(
      BSONDocument("user" -> user.id),
      BSONDocument(field -> true)
    ).cursor[BSONDocument]().collect[List]() map {
        _.flatMap(_.getAs[Value](field)).toSet
      }

  private def nextUsers(field: String)(values: Set[Value], user: User): Fu[Set[User]] =
    values.nonEmpty ?? {
      storeColl.find(
        BSONDocument(
          field -> BSONDocument("$in" -> values),
          "user" -> BSONDocument("$ne" -> user.id)
        ),
        BSONDocument("user" -> true)
      ).cursor[BSONDocument]().collect[List]() map {
          _.flatMap(_.getAs[String]("user"))
        } flatMap { userIds =>
          userIds.nonEmpty ?? (UserRepo byIds userIds.distinct) map (_.toSet)
        }
    }
}
