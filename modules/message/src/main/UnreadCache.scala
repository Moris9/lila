package lila.message

import spray.caching.{ LruCache, Cache }

import lila.user.User

private[message] final class UnreadCache {

  // userId => nb unread
  private val cache: Cache[Int] = LruCache(maxCapacity = 99999)

  def apply(userId: String): Fu[Int] =
    cache.fromFuture(userId.toLowerCase)(ThreadRepo userNbUnread userId)

  def refresh(userId: String): Fu[Int] = 
    (cache remove userId).fold(apply(userId))(_ >> apply(userId))
}
