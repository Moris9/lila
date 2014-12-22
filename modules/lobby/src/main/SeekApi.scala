package lila.lobby

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import reactivemongo.core.commands._
import scala.concurrent.duration._

import actorApi.LobbyUser
import lila.db.Types.Coll
import lila.memo.AsyncCache
import lila.user.{ User, UserRepo }

final class SeekApi(
    coll: Coll,
    blocking: String => Fu[Set[String]],
    maxPerPage: Int,
    maxPerUser: Int) {

  private sealed trait CacheKey
  private object ForAnon extends CacheKey
  private object ForUser extends CacheKey

  private def allCursor =
    coll.find(BSONDocument())
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Seek]

  private val cache = AsyncCache[CacheKey, List[Seek]](
    f = {
      case ForAnon => allCursor.collect[List](maxPerPage)
      case ForUser => allCursor.collect[List]()
    },
    timeToLive = 5.seconds)

  def forAnon = cache(ForAnon)

  def forUser(user: User): Fu[List[Seek]] =
    blocking(user.id) flatMap { blocking =>
      forUser(LobbyUser.make(user, blocking))
    }

  def forUser(user: LobbyUser): Fu[List[Seek]] = cache(ForUser) map {
    _ filter { seek =>
      seek.user.id == user.id || Biter.canJoin(seek, user)
    } take maxPerPage
  }

  def find(id: String): Fu[Option[Seek]] =
    coll.find(BSONDocument("_id" -> id)).one[Seek]

  def insert(seek: Seek) = coll.insert(seek) >> findByUser(seek.user.id).flatMap {
    case seeks if seeks.size <= maxPerUser => funit
    case seeks =>
      seeks.drop(maxPerUser).map(remove).sequenceFu >> cache.clear
  }

  def findByUser(userId: String): Fu[List[Seek]] =
    coll.find(BSONDocument("user.id" -> userId))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Seek].collect[List]()

  def remove(seek: Seek) =
    coll.remove(BSONDocument("_id" -> seek.id)).void >> cache.clear

  def removeBy(seekId: String, userId: String) =
    coll.remove(BSONDocument(
      "_id" -> seekId,
      "user.id" -> userId
    )).void >> cache.clear
}
