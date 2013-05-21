package lila.app
package templating

import lila.user.Context

trait FriendHelper {

  private def api = Env.friend.api

  def areFriends(u1: String, u2: String) = api.areFriends(u1, u2).await

  def isFriend(u: String)(implicit ctx: Context) = ctx.userId ?? { areFriends(_, u) }

  def friendRequesters(implicit ctx: Context): List[String] =
    (ctx.userId ?? api.requestersOf).await

  def friendStatus(u: String)(implicit ctx: Context) =
    ctx.userId map { api.quickStatus(_, u).await }
}
