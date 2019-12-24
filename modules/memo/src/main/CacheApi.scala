package lila.memo

import akka.actor.ActorSystem
import com.github.benmanes.caffeine
import com.github.blemale.scaffeine._
import play.api.Mode
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

final class CacheApi(mode: Mode)(implicit ec: ExecutionContext, system: ActorSystem) {

  import CacheApi._

  def apply[K, V](initialCapacity: Int, name: String)(
      build: Builder => AsyncLoadingCache[K, V]
  ): AsyncLoadingCache[K, V] = {
    val actualCapacity =
      if (mode != Mode.Prod) math.sqrt(initialCapacity).toInt atLeast 1
      else initialCapacity
    val cache = build {
      scaffeine.recordStats.initialCapacity(actualCapacity)
    }
    monitor(name, cache)
    cache
  }

  def unit[V](build: Builder => AsyncLoadingCache[Unit, V]): AsyncLoadingCache[Unit, V] = {
    build(scaffeine initialCapacity 1)
  }

  def sync[K, V](
      name: String,
      initialCapacity: Int,
      compute: K => Fu[V],
      default: K => V,
      strategy: Syncache.Strategy,
      expireAfter: Syncache.ExpireAfter
  ): Syncache[K, V] = {
    val actualCapacity =
      if (mode != Mode.Prod) math.sqrt(initialCapacity).toInt atLeast 1
      else initialCapacity
    val cache = new Syncache(name, actualCapacity, compute, default, strategy, expireAfter)
    monitor(name, cache.cache)
    cache
  }

  def monitor(name: String, cache: AsyncCache[_, _]): Unit =
    monitor(name, cache.underlying.synchronous)

  def monitor(name: String, cache: Cache[_, _]): Unit =
    monitor(name, cache.underlying)

  def monitor(name: String, cache: caffeine.cache.Cache[_, _]): Unit =
    startMonitor(name, cache)
}

object CacheApi {

  private type Builder = Scaffeine[Any, Any]

  def scaffeine: Builder = Scaffeine().scheduler(caffeine.cache.Scheduler.systemScheduler)

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
