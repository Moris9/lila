package lila.chat

import akka.actor.{ ActorSystem, Props, ActorSelection }
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    flood: lila.security.Flood,
    shutup: ActorSelection,
    system: ActorSystem) {

  private val settings = new {
    val CollectionChat = config getString "collection.chat"
    val CollectionTimeout = config getString "collection.timeout"
    val MaxLinesPerChat = config getInt "max_lines"
    val NetDomain = config getString "net.domain"
    val ActorName = config getString "actor.name"
  }
  import settings._

  val api = new ChatApi(
    coll = chatColl,
    flood = flood,
    shutup = shutup,
    maxLinesPerChat = MaxLinesPerChat,
    netDomain = NetDomain)

  private val timeout = new ChatTimeout(
    chatColl = chatColl,
    timeoutColl = timeoutColl)

  system.actorOf(Props(new FrontActor(api, timeout)), name = ActorName)

  private[chat] lazy val chatColl = db(CollectionChat)
  private[chat] lazy val timeoutColl = db(CollectionTimeout)
}

object Env {

  lazy val current: Env = "chat" boot new Env(
    config = lila.common.PlayApp loadConfig "chat",
    db = lila.db.Env.current,
    flood = lila.security.Env.current.flood,
    shutup = lila.hub.Env.current.actor.shutup,
    system = lila.common.PlayApp.system)
}
