package lila.explorer

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    gameColl: lila.db.dsl.Coll,
    gameImporter: lila.importer.Importer,
    settingStore: lila.memo.SettingStore.Builder,
    system: ActorSystem
) {

  private val InternalEndpoint = config getString "internal_endpoint"

  private lazy val indexer = new ExplorerIndexer(
    gameColl = gameColl,
    internalEndpoint = InternalEndpoint
  )

  lazy val importer = new ExplorerImporter(
    endpoint = InternalEndpoint,
    gameImporter = gameImporter
  )

  def cli = new lila.common.Cli {
    def process = {
      case "explorer" :: "index" :: since :: Nil => indexer(since) inject "done"
    }
  }

  lazy val indexFlowSetting = settingStore[Boolean](
    "explorerIndexFlow",
    default = true,
    text = "Explorer: index new games as soon as they complete".some
  )

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted && indexFlowSetting.get() => indexer(game)
    }
  })), 'finishGame)
}

object Env {

  lazy val current = "explorer" boot new Env(
    config = lila.common.PlayApp loadConfig "explorer",
    gameColl = lila.game.Env.current.gameColl,
    gameImporter = lila.importer.Env.current.importer,
    settingStore = lila.memo.Env.current.settingStore,
    system = lila.common.PlayApp.system
  )
}
