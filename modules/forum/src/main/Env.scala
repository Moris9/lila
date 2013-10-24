package lila.forum

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.hub.actorApi.forum._
import lila.mod.ModlogApi

final class Env(
    config: Config,
    db: lila.db.Env,
    modLog: ModlogApi,
    hub: lila.hub.Env,
    system: ActorSystem) {

  private val settings = new {
    val TopicMaxPerPage = config getInt "topic.max_per_page"
    val PostMaxPerPage = config getInt "post.max_per_page"
    val RecentTtl = config duration "recent.ttl"
    val RecentNb = config getInt "recent.nb"
    val CollectionCateg = config getString "collection.categ"
    val CollectionTopic = config getString "collection.topic"
    val CollectionPost = config getString "collection.post"
    val ActorName = config getString "actor.name"
  }
  import settings._

  lazy val categApi = new CategApi(env = this)

  lazy val topicApi = new TopicApi(
    env = this,
    indexer = hub.actor.forumIndexer,
    maxPerPage = TopicMaxPerPage,
    modLog = modLog,
    timeline = hub.actor.timeline)

  lazy val postApi = new PostApi(
    env = this,
    indexer = hub.actor.forumIndexer,
    maxPerPage = PostMaxPerPage,
    modLog = modLog,
    timeline = hub.actor.timeline)

  lazy val forms = new DataForm(hub.actor.captcher)
  lazy val recent = new Recent(postApi, RecentTtl, RecentNb)

  def cli = new lila.common.Cli {
    import tube._
    def process = {
      case "forum" :: "denormalize" :: Nil ⇒
        topicApi.denormalize >> categApi.denormalize inject "Forum denormalized"
      case "forum" :: "typecheck" :: Nil ⇒
        lila.db.Typecheck.apply[Categ] >>
          lila.db.Typecheck.apply[Topic] >>
          lila.db.Typecheck.apply[Post]
    }
  }

  system.actorOf(Props(new Actor {
    def receive = {
      case MakeTeam(id, name) ⇒ categApi.makeTeam(id, name)
    }
  }), name = ActorName)

  private[forum] lazy val categColl = db(CollectionCateg)
  private[forum] lazy val topicColl = db(CollectionTopic)
  private[forum] lazy val postColl = db(CollectionPost)
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "[boot] forum" describes new Env(
    config = lila.common.PlayApp loadConfig "forum",
    db = lila.db.Env.current,
    modLog = lila.mod.Env.current.logApi,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system)
}
