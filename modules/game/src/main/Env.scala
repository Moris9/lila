package lila.game

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.config._

final private class GameConfig(
    @ConfigName("collection.game") val gameColl: CollName,
    @ConfigName("collection.crosstable") val crosstableColl: CollName,
    @ConfigName("collection.matchup") val matchupColl: CollName,
    @ConfigName("paginator.max_per_page") val paginatorMaxPerPage: MaxPerPage,
    @ConfigName("captcher.name") val captcherName: String,
    @ConfigName("captcher.duration") val captcherDuration: FiniteDuration,
    val gifUrl: String
)

@Module
final class Env(
    appConfig: Configuration,
    ws: WSClient,
    db: lila.db.Db,
    baseUrl: BaseUrl,
    userRepo: lila.user.UserRepo,
    mongoCache: lila.memo.MongoCache.Api,
    lightUserApi: lila.user.LightUserApi,
    settingStore: lila.memo.SettingStore.Builder,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext, system: ActorSystem, scheduler: Scheduler) {

  private val config = appConfig.get[GameConfig]("game")(AutoConfig.loader)
  import config.paginatorMaxPerPage

  lazy val gameRepo = new GameRepo(db(config.gameColl))

  lazy val idGenerator = wire[IdGenerator]

  lazy val gifExport = new GifExport(ws, lightUserApi, baseUrl, config.gifUrl)

  lazy val divider = wire[Divider]

  lazy val cached: Cached = wire[Cached]

  lazy val paginator = wire[PaginatorBuilder]

  lazy val uciMemo = wire[UciMemo]

  lazy val pgnDump = wire[PgnDump]

  lazy val playTimeInit = settingStore[Boolean](
    "playTimeInit",
    default = true,
    text = "Compute empty play times (DB expensive)".some
  )

  lazy val playTimeCompute = new PlayTimeApi.Compute(playTimeInit.get _)

  lazy val crosstableApi = new CrosstableApi(
    coll = db(config.crosstableColl),
    matchupColl = db(config.matchupColl)
  )

  lazy val playTime = wire[PlayTimeApi]

  lazy val gamesByUsersStream = wire[GamesByUsersStream]

  lazy val bestOpponents = wire[BestOpponents]

  lazy val rematches = Rematches(
    lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterWrite(1 hour)
      .build[Game.ID, Game.ID]
  )

  lazy val jsonView = wire[JsonView]

  // eargerly load captcher actor
  private val captcher = system.actorOf(Props(new Captcher(gameRepo)), name = config.captcherName)
  scheduler.scheduleWithFixedDelay(config.captcherDuration, config.captcherDuration) { () =>
    captcher ! actorApi.NewCaptcha
  }
}
