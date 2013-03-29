package lila.timeline

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    getUsername: String ⇒ Fu[String],
    sockets: ActorRef) {

  private val CollectionEntry = config getString "collection.entry"
  private val DisplayMax = config getString "display_max"

  private[timeline] lazy val entryColl = db(CollectionEntry)
}

object Env {

  lazy val current = "[timeline] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "timeline",
    db = lila.db.Env.current,
    getUsername = lila.user.Env.current.usernameOrAnonymous _,
    sockets = lila.hub.Env.current.sockets)
}
