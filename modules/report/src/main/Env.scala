package lila.report

import scala.concurrent.duration._
import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    isOnline: lila.user.User.ID => Boolean,
    noteApi: lila.user.NoteApi,
    securityApi: lila.security.SecurityApi,
    system: ActorSystem,
    hub: lila.hub.Env,
    settingStore: lila.memo.SettingStore.Builder,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val CollectionReport = config getString "collection.report"
  private val ActorName = config getString "actor.name"
  private val ScoreThreshold = config getInt "score.threshold"

  val scoreThresholdSetting = settingStore[Int](
    "reportScoreThreshold",
    default = ScoreThreshold,
    text = "Report score threshold. Reports with lower scores are concealed to moderators".some
  )

  lazy val forms = new DataForm(hub.actor.captcher)

  private lazy val autoAnalysis = new AutoAnalysis(
    fishnet = hub.actor.fishnet,
    system = system
  )

  lazy val api = new ReportApi(
    reportColl,
    autoAnalysis,
    noteApi,
    securityApi,
    isOnline,
    asyncCache,
    system.lilaBus,
    scoreThreshold = scoreThresholdSetting.get
  )

  lazy val modFilters = new ModReportFilter

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.report.Cheater(userId, text) =>
        api.autoCheatReport(userId, text)
      case lila.hub.actorApi.report.Shutup(userId, text) =>
        api.autoInsultReport(userId, text)
      case lila.hub.actorApi.report.Booster(winnerId, loserId) =>
        api.autoBoostReport(winnerId, loserId)
    }
  }), name = ActorName)

  system.scheduler.schedule(1 minute, 1 minute) { api.inquiries.expire }

  lazy val reportColl = db(CollectionReport)
}

object Env {

  lazy val current = "report" boot new Env(
    config = lila.common.PlayApp loadConfig "report",
    db = lila.db.Env.current,
    isOnline = lila.user.Env.current.isOnline,
    noteApi = lila.user.Env.current.noteApi,
    securityApi = lila.security.Env.current.api,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    settingStore = lila.memo.Env.current.settingStore,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
