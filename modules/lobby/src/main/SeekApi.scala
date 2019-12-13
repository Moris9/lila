package lila.lobby

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.config._
import lila.db.dsl._
import lila.user.User

final class SeekApi(
    config: SeekApi.Config,
    biter: Biter,
    relationApi: lila.relation.RelationApi,
    asyncCache: lila.memo.AsyncCache.Builder
) {
  import config._

  sealed private trait CacheKey
  private object ForAnon extends CacheKey
  private object ForUser extends CacheKey

  private def allCursor =
    coll.ext
      .find($empty)
      .sort($doc("createdAt" -> -1))
      .cursor[Seek]()

  private val cache = asyncCache.clearable[CacheKey, List[Seek]](
    name = "lobby.seek.list",
    f = {
      case ForAnon => allCursor.gather[List](maxPerPage.value)
      case ForUser => allCursor.gather[List]()
    },
    maxCapacity = 2,
    expireAfter = _.ExpireAfterWrite(3.seconds)
  )

  private def cacheClear() = {
    cache invalidate ForAnon
    cache invalidate ForUser
  }

  def forAnon = cache get ForAnon

  def forUser(user: User): Fu[List[Seek]] =
    relationApi.fetchBlocking(user.id) flatMap { blocking =>
      forUser(LobbyUser.make(user, blocking))
    }

  def forUser(user: LobbyUser): Fu[List[Seek]] = cache get ForUser map { seeks =>
    val filtered = seeks.filter { seek =>
      seek.user.id == user.id || biter.canJoin(seek, user)
    }
    noDupsFor(user, filtered) take maxPerPage.value
  }

  private def noDupsFor(user: LobbyUser, seeks: List[Seek]) =
    seeks
      .foldLeft(List.empty[Seek] -> Set.empty[String]) {
        case ((res, h), seek) if seek.user.id == user.id => (seek :: res, h)
        case ((res, h), seek) =>
          val seekH = List(seek.variant, seek.daysPerTurn, seek.mode, seek.color, seek.user.id) mkString ","
          if (h contains seekH) (res, h)
          else (seek :: res, h + seekH)
      }
      ._1
      .reverse

  def find(id: String): Fu[Option[Seek]] =
    coll.ext.find($id(id)).one[Seek]

  def insert(seek: Seek) =
    coll.insert.one(seek) >> findByUser(seek.user.id).flatMap {
      case seeks if maxPerUser >= seeks.size => funit
      case seeks                             => seeks.drop(maxPerUser.value).map(remove).sequenceFu
    }.void >>- cacheClear

  def findByUser(userId: String): Fu[List[Seek]] =
    coll.ext
      .find($doc("user.id" -> userId))
      .sort($doc("createdAt" -> -1))
      .cursor[Seek]()
      .gather[List]()

  def remove(seek: Seek) =
    coll.delete.one($doc("_id" -> seek.id)).void >>- cacheClear

  def archive(seek: Seek, gameId: String) = {
    val archiveDoc = Seek.seekBSONHandler.writeTry(seek).get ++ $doc(
      "gameId"     -> gameId,
      "archivedAt" -> DateTime.now
    )
    coll.delete.one($doc("_id" -> seek.id)).void >>-
      cacheClear >>
      archiveColl.insert.one(archiveDoc)
  }

  def findArchived(gameId: String): Fu[Option[Seek]] =
    archiveColl.ext.find($doc("gameId" -> gameId)).one[Seek]

  def removeBy(seekId: String, userId: String) =
    coll.delete
      .one(
        $doc(
          "_id"     -> seekId,
          "user.id" -> userId
        )
      )
      .void >>- cacheClear

  def removeByUser(user: User) =
    coll.delete.one($doc("user.id" -> user.id)).void >>- cacheClear
}

private object SeekApi {

  final class Config(
      val coll: Coll,
      val archiveColl: Coll,
      val maxPerPage: MaxPerPage,
      val maxPerUser: Max
  )
}
