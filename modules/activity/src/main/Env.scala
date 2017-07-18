package lila.activity

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: akka.actor.ActorSystem
) {

  private val activityColl = db(config getString "collection.activity")

  val api = new ActivityApi(
    coll = activityColl
  )

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted => api addGame game
    }
  })), 'finishGame)
}

object Env {

  lazy val current: Env = "activity" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "activity",
    system = lila.common.PlayApp.system
  )
}
