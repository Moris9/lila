package lila.oauth

import scala.concurrent.duration._
import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.common.Bearer
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class AccessTokenApi(coll: Coll, cacheApi: lila.memo.CacheApi, userRepo: UserRepo)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import OAuthScope.scopeHandler
  import AccessToken.{ BSONFields => F }

  def create(token: AccessToken): Funit = coll.insert.one(token).void

  def create(setup: OAuthForm.token.Data, me: User, isStudent: Boolean): Funit =
    (fuccess(isStudent) >>| userRepo.isManaged(me.id)) flatMap { noBot =>
      val token = setup make me
      create(
        token.copy(
          scopes = token.scopes.filterNot(_ == OAuthScope.Bot.Play && noBot)
        )
      )
    }

  def create(granted: AccessTokenRequest.Granted): Fu[AccessToken] = {
    val plain = Bearer.random()
    val token = AccessToken(
      id = AccessToken.Id.from(plain),
      plain = plain,
      userId = granted.userId,
      description = None,
      createdAt = DateTime.now().some,
      scopes = granted.scopes,
      clientOrigin = granted.redirectUri.clientOrigin.some,
      expires = DateTime.now().plusMonths(12).some
    )
    create(token) inject token
  }

  def listPersonal(user: User): Fu[List[AccessToken]] =
    coll
      .find(
        $doc(
          F.userId       -> user.id,
          F.clientOrigin -> $exists(false)
        )
      )
      .sort($sort desc F.createdAt)
      .cursor[AccessToken]()
      .list(100)

  def countPersonal(user: User): Fu[Int] =
    coll.countSel(
      $doc(
        F.userId       -> user.id,
        F.clientOrigin -> $exists(false)
      )
    )

  def findCompatiblePersonal(user: User, scopes: Set[OAuthScope]): Fu[Option[AccessToken]] =
    coll.one[AccessToken](
      $doc(
        F.userId       -> user.id,
        F.clientOrigin -> $exists(false),
        F.scopes $all scopes.toSeq
      )
    )

  def listClients(user: User, limit: Int): Fu[List[AccessTokenApi.Client]] =
    coll.aggregateList(limit) { framework =>
      import framework._
      Match(
        $doc(
          F.userId       -> user.id,
          F.clientOrigin -> $exists(true)
        )
      ) -> List(
        Unwind(path = F.scopes, includeArrayIndex = None, preserveNullAndEmptyArrays = Some(true)),
        GroupField(F.clientOrigin)(
          F.usedAt -> MaxField(F.usedAt),
          F.scopes -> AddFieldToSet(F.scopes)
        ),
        Sort(Descending(F.usedAt))
      )
    } map { docs =>
      for {
        doc    <- docs
        origin <- doc.getAsOpt[String]("_id")
        usedAt = doc.getAsOpt[DateTime](F.usedAt)
        scopes <- doc.getAsOpt[List[OAuthScope]](F.scopes)
      } yield AccessTokenApi.Client(origin, usedAt, scopes)
    }

  def revoke(id: AccessToken.Id): Funit =
    coll.delete.one($id(id)).map(_ => invalidateCached(id))

  def revokeByClientOrigin(clientOrigin: String, user: User): Funit =
    coll
      .find(
        $doc(
          F.userId       -> user.id,
          F.clientOrigin -> clientOrigin
        ),
        $doc(F.id -> 1).some
      )
      .sort($sort desc F.usedAt)
      .cursor[Bdoc]()
      .list(100)
      .flatMap { invalidate =>
        coll.delete
          .one(
            $doc(
              F.userId       -> user.id,
              F.clientOrigin -> clientOrigin
            )
          )
          .map(_ => invalidate.flatMap(_.getAsOpt[AccessToken.Id](F.id)).foreach(invalidateCached))
      }

  def get(bearer: Bearer) = accessTokenCache.get(AccessToken.Id.from(bearer))

  private val accessTokenCache =
    cacheApi[AccessToken.Id, Option[AccessToken.ForAuth]](32, "oauth.access_token") {
      _.expireAfterWrite(5 minutes)
        .buildAsyncFuture(fetchAccessToken)
    }

  private def fetchAccessToken(id: AccessToken.Id): Fu[Option[AccessToken.ForAuth]] =
    coll.ext.findAndUpdate[AccessToken.ForAuth](
      selector = $id(id),
      update = $set(F.usedAt -> DateTime.now()),
      fields = AccessToken.forAuthProjection.some
    )

  private def invalidateCached(id: AccessToken.Id): Unit =
    accessTokenCache.put(id, fuccess(none))
}

object AccessTokenApi {
  case class Client(
      origin: String,
      usedAt: Option[DateTime],
      scopes: List[OAuthScope]
  )
}
