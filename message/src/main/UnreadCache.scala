package lila.message

import lila.user.User

import spray.caching.{ LruCache, Cache }
import scala.collection.mutable

import play.api.libs.concurrent.Execution.Implicits._

private[message] final class UnreadCache(threadRepo: ThreadRepo) {

  // userId => nb unread
  private val cache: Cache[Int] = LruCache(maxCapacity = 99999)

  def apply(userId: String): Fu[Int] =
    cache.fromFuture(userId.toLowerCase)(threadRepo userNbUnread userId)

  def refresh(userId: String): Fu[Int] = 
    (cache remove userId).fold(apply(userId))(_ >> apply(userId))
}
