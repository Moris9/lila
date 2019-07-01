package lila.socket

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._
import scala.concurrent.Future

import actorApi._

final class Env(
    system: ActorSystem,
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle,
    settingStore: lila.memo.SettingStore.Builder
) {

  private val settings = new {
    val RedisHost = config getString "redis.host"
    val RedisPort = config getInt "redis.port"
  }
  import settings._

  private val population = new Population(system)

  private val moveBroadcast = new MoveBroadcast(system)

  private val userRegister = new UserRegister(system)

  private val redis = new scredis.Redis(host = RedisHost, port = RedisPort)

  private val remoteSocket = new RemoteSocket(
    redis = redis,
    chanIn = "site-in",
    chanOut = "site-out",
    bus = system.lilaBus
  )

  lifecycle.addStopHook { () =>
    logger.info("Stopping the Redis client...")
    Future(redis.quit())
  }

  system.scheduler.schedule(5 seconds, 1 seconds) { population ! PopulationTell }

  val socketDebugSetting = settingStore[Boolean](
    "socketDebug",
    default = false,
    text = "Send extra debugging to websockets.".some
  )
}

object Env {

  lazy val current = "socket" boot new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "socket",
    lifecycle = lila.common.PlayApp.lifecycle,
    settingStore = lila.memo.Env.current.settingStore
  )
}
