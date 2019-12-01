package lila.report

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.common.Domain

@Module
private class CoachConfig(
    @ConfigName("collection.report") val reportColl: CollName,
    @ConfigName("score.threshold") val scoreThreshold: Int,
    @ConfigName("net.domain") val netDomain: Domain,
    @ConfigName("actor.name") val actorName: String
)

private case class Thresholds(score: () => Int, slack: () => Int)

final class Env(
    appConfig: Configuration,
    db: lila.db.Env,
    isOnline: lila.user.User.ID => Boolean,
    noteApi: lila.user.NoteApi,
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo,
    securityApi: lila.security.SecurityApi,
    userSpyApi: lila.security.UserSpyApi,
    playbanApi: lila.playban.PlaybanApi,
    slackApi: lila.slack.SlackApi,
    captcher: lila.hub.actors.Captcher,
    fishnet: lila.hub.actors.Fishnet,
    settingStore: lila.memo.SettingStore.Builder,
    asyncCache: lila.memo.AsyncCache.Builder
)(implicit system: ActorSystem) {

  private val config = appConfig.get[CoachConfig]("coach")(AutoConfig.loader)

  private lazy val reportColl = db(config.reportColl)

  val scoreThresholdSetting = settingStore[Int](
    "reportScoreThreshold",
    default = config.scoreThreshold,
    text = "Report score threshold. Reports with lower scores are concealed to moderators".some
  )

  val slackScoreThresholdSetting = settingStore[Int](
    "slackScoreThreshold",
    default = 80,
    text = "Slack score threshold. Comm reports with higher scores are notified in slack".some
  )

  private val thresholds = Thresholds(
    score = scoreThresholdSetting.get,
    slack = slackScoreThresholdSetting.get
  )

  lazy val forms = wire[DataForm]

  private lazy val autoAnalysis = wire[AutoAnalysis]

  lazy val api = wire[ReportApi]

  lazy val modFilters = new ModReportFilter

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.report.Cheater(userId, text) =>
        api.autoCheatReport(userId, text)
      case lila.hub.actorApi.report.Shutup(userId, text, major) =>
        api.autoInsultReport(userId, text, major)
      case lila.hub.actorApi.report.Booster(winnerId, loserId) =>
        api.autoBoostReport(winnerId, loserId)
    }
  }), name = config.actorName)

  lila.common.Bus.subscribeFun("playban") {
    case lila.hub.actorApi.playban.Playban(userId, _) => api.maybeAutoPlaybanReport(userId)
  }

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () => api.inquiries.expire }
}
