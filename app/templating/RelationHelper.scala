package lila.app
package templating

import akka.pattern.ask
import play.api.libs.json._

import lila.hub.actorApi.relation._
import lila.relation.Relation
import lila.user.Context
import makeTimeout.short

trait RelationHelper {

  private def api = Env.relation.api

  def relationWith(userId: String)(implicit ctx: Context): Option[Relation] =
    ctx.userId flatMap { api.relation(_, userId).await }

  def followsMe(userId: String)(implicit ctx: Context): Boolean =
    ctx.userId ?? { api.follows(userId, _).await }

  def onlineFriends(userId: String): JsObject = {
    Env.hub.actor.relation ? GetOnlineFriends(userId) map {
      case OnlineFriends(usernames, nb) ⇒ Json.obj(
        "us" -> usernames,
        "nb" -> nb
      )
    }
  }.await

  def nbFollowers(userId: String) =
    Env.relation.api.nbFollowers(userId).await

  def nbFollowing(userId: String) =
    Env.relation.api.nbFollowing(userId).await
}
