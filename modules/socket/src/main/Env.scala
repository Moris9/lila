package lila.socket

import akka.actor._
import akka.pattern.{ ask, pipe }
import com.typesafe.config.Config

import actorApi._
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  import scala.concurrent.duration._

  private val PopulationName = config getString "population.name"
  private val HubName = config getString "hub.name"

  private val socketHub =
    system.actorOf(Props[SocketHub], name = HubName)

  private val population =
    system.actorOf(Props[Population], name = PopulationName)

  private val bus = system.lilaBus

  scheduler.once(5 seconds) {
    scheduler.message(4 seconds) {
      socketHub -> actorApi.Broom
    }
    scheduler.effect(1 seconds, "calculate nb members") {
      population ? PopulationGet foreach {
        case nb: Int ⇒ bus.publish(NbMembers(nb), 'nbMembers)
      }
    }
  }
}

object Env {

  lazy val current = "[boot] socket" describes new Env(
    config = lila.common.PlayApp loadConfig "socket",
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
