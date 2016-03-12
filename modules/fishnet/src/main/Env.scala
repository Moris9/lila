package lila.fishnet

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    uciMemo: lila.game.UciMemo,
    hub: lila.hub.Env,
    db: lila.db.Env,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val moveColl = db(config getString "collection.move")
  private val analysisColl = db(config getString "collection.analysis")
  private val clientColl = db(config getString "collection.client")

  private val sequencer = new lila.hub.FutureSequencer(
    system = system,
    receiveTimeout = None,
    executionTimeout = Some(300 millis))

  val api = new FishnetApi(
    hub = hub,
    moveColl = moveColl,
    analysisColl = analysisColl,
    clientColl = clientColl,
    sequencer = sequencer)

  val player = new Player(
    api = api,
    uciMemo = uciMemo,
    sequencer = sequencer)

  private val cleaner = new Cleaner(
    api = api,
    moveColl = moveColl,
    analysisColl = analysisColl,
    scheduler = scheduler)

  def cli = new lila.common.Cli {
    def process = {
      case "fishnet" :: "add" :: "client" :: key :: userId :: skill :: Nil =>
        api.createClient(key, userId, skill) inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "fishnet" boot new Env(
    system = lila.common.PlayApp.system,
    uciMemo = lila.game.Env.current.uciMemo,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "fishnet",
    scheduler = lila.common.PlayApp.scheduler)
}
