package lila.memo

import akka.actor.ActorSystem
import com.github.benmanes.caffeine
import com.github.blemale.scaffeine._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

final class CacheApi(implicit ec: ExecutionContext, system: ActorSystem) {

  private type Builder = Scaffeine[Any, Any]

  val scaffeine: Builder = Scaffeine().scheduler(caffeine.cache.Scheduler.systemScheduler)

  def apply[K, V](name: String)(build: Builder => AsyncLoadingCache[K, V]): AsyncLoadingCache[K, V] = {
    val cache = build(scaffeine)
    monitor(name, cache)
    cache
  }

  def unit[V](build: Builder => AsyncLoadingCache[Unit, V]): AsyncLoadingCache[Unit, V] = {
    build(scaffeine.initialCapacity(1))
  }

  def monitor(name: String, cache: AsyncCache[_, _]): Unit =
    monitor(name, cache.underlying.synchronous)

  def monitor(name: String, cache: Cache[_, _]): Unit =
    monitor(name, cache.underlying)

  def monitor(name: String, cache: caffeine.cache.Cache[_, _]): Unit =
    CacheApi.startMonitor(name, cache)
}

object CacheApi {

  implicit def beafedAsync[K, V](cache: AsyncCache[K, V])     = new BeafedAsync[K, V](cache)
  implicit def beafedAsyncUnit[V](cache: AsyncCache[Unit, V]) = new BeafedAsyncUnit[V](cache)
  implicit def beafedAsyncLoadingUnit[V](cache: AsyncLoadingCache[Unit, V]) =
    new BeafedAsyncLoadingUnit[V](cache)

  private[memo] def startMonitor(
      name: String,
      cache: caffeine.cache.Cache[_, _]
  )(implicit ec: ExecutionContext, system: ActorSystem): Unit =
    system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
      lila.mon.caffeineStats(cache, name)
    }
}

final class BeafedAsync[K, V](val cache: AsyncCache[K, V]) extends AnyVal {

  def invalidate(key: K): Unit = cache.underlying.synchronous invalidate key
  def invalidateAll(): Unit    = cache.underlying.synchronous.invalidateAll()

  def update(key: K, f: V => V): Unit = cache.getIfPresent(key) foreach { v =>
    cache.put(key, v dmap f)
  }
}

final class BeafedAsyncUnit[V](val cache: AsyncCache[Unit, V]) extends AnyVal {

  def invalidateUnit(): Unit = cache.underlying.synchronous.invalidate({})
}

final class BeafedAsyncLoadingUnit[V](val cache: AsyncLoadingCache[Unit, V]) extends AnyVal {

  def getUnit: Fu[V] = cache.get({})
}
