package lila.forum

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._
import lila.common.DetectLanguage
import lila.hub.actorApi.team.CreateTeam
import lila.mod.ModlogApi
import lila.notify.NotifyApi
import lila.relation.RelationApi

@Module
final private class ForumConfig(
    @ConfigName("topic.max_per_page") val topicMaxPerPage: MaxPerPage,
    @ConfigName("post.max_per_page") val postMaxPerPage: MaxPerPage,
    @ConfigName("public_categ_ids") val publicCategIds: List[String]
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    modLog: ModlogApi,
    spam: lila.security.Spam,
    captcher: lila.hub.actors.Captcher,
    timeline: lila.hub.actors.Timeline,
    shutup: lila.hub.actors.Shutup,
    forumSearch: lila.hub.actors.ForumSearch,
    detectLanguage: DetectLanguage,
    notifyApi: NotifyApi,
    relationApi: RelationApi,
    userRepo: lila.user.UserRepo,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val config = appConfig.get[ForumConfig]("forum")(AutoConfig.loader)

  lazy val categRepo = new CategRepo(db(CollName("f_categ")))
  lazy val topicRepo = new TopicRepo(db(CollName("f_topic")))
  lazy val postRepo  = new PostRepo(db(CollName("f_post")))

  lazy val categApi: CategApi = {
    val mk = (env: Env) => wire[CategApi]
    mk(this)
  }

  lazy val topicApi: TopicApi = {
    val mk = (max: MaxPerPage, env: Env) => wire[TopicApi]
    mk(config.topicMaxPerPage, this)
  }

  lazy val postApi: PostApi = {
    val mk = (max: MaxPerPage, env: Env) => wire[PostApi]
    mk(config.postMaxPerPage, this)
  }

  lazy val mentionNotifier: MentionNotifier = wire[MentionNotifier]
  lazy val forms                            = wire[DataForm]
  lazy val recent                           = wire[Recent]

  lila.common.Bus.subscribeFun("team", "gdprErase") {
    case CreateTeam(id, name, _)        => categApi.makeTeam(id, name)
    case lila.user.User.GDPRErase(user) => postApi erase user
  }
}
