package lila.site

import lila.common.PimpedConfig._
import akka.actor._
import com.typesafe.config.Config
import play.api.Play.current
import play.api.libs.concurrent.Akka.system

final class Env(config: Config, hub: lila.hub.Env) {

  private val SocketUidTtl = config duration "socket.uid.ttl"
  private val SocketName = config getString "socket.name"

  private val socket = system.actorOf(
    Props(new Socket(timeout = SocketUidTtl)), name = SocketName)

  lazy val socketHandler = new SocketHandler(socket, hub)
}

object Env {

  lazy val current = "[boot] site" describes new Env(
    config = lila.common.PlayApp loadConfig "site",
    hub = lila.hub.Env.current)
}
