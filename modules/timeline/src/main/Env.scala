package lila.timeline

import tube.entryTube
import lila.db.api.$find

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    getUsername: String ⇒ Fu[String],
    lobbySocket: ActorRef,
    system: ActorSystem) {

  private val CollectionEntry = config getString "collection.entry"
  private val DisplayMax = config getInt "display_max"
  private val ActorName = config getString "actor.name"

  def recent = $find recent DisplayMax

  private lazy val push = system.actorOf(Props(new Push(
    lobbySocket = lobbySocket,
    getUsername = getUsername
  )), name = ActorName)

  private[timeline] lazy val entryColl = db(CollectionEntry)
}

object Env {

  lazy val current = "[boot] timeline" describes new Env(
    config = lila.common.PlayApp loadConfig "timeline",
    db = lila.db.Env.current,
    getUsername = lila.user.Env.current.usernameOrAnonymous _,
    lobbySocket = lila.hub.Env.current.socket.lobby,
    system = lila.common.PlayApp.system)
}
