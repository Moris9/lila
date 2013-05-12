package lila.message

import lila.hub.actorApi.message.LichessThread

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env, 
    system: ActorSystem) {

  private val CollectionThread = config getString "collection.thread"
  private val ThreadMaxPerPage = config getInt "thread.max_per_page"
  private val ActorName = config getString "actor.name"

  private[message] lazy val threadColl = db(CollectionThread)

  private lazy val unreadCache = new UnreadCache

  lazy val forms = new DataForm

  lazy val api = new Api(
    unreadCache = unreadCache,
    maxPerPage = ThreadMaxPerPage,
    socketHub = hub.socket.hub)

  system.actorOf(Props(new Actor {
    def receive = {
      case thread: LichessThread ⇒ api.lichessThread(thread)
    }
  }), name = ActorName)

  def cli = new lila.common.Cli {
    import lila.db.api.$find
    import tube.threadTube
    def process = {
      case "message" :: "typecheck" :: Nil ⇒
        $find.all[Thread] inject "Messages type checked"
    }
  }
}

object Env {

  lazy val current = "[boot] message" describes new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system)
}
