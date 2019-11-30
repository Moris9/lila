package lila.game

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.config._

@Module
private class GameConfig(
    @ConfigName("collection.game") val gameColl: CollName,
    @ConfigName("collection.crosstable") val crosstableColl: CollName,
    @ConfigName("collection.matchup") val matchupColl: CollName,
    @ConfigName("paginator.maxPerPage") val paginatorMaxPerPage: MaxPerPage,
    @ConfigName("captcher.name") val captcherName: String,
    @ConfigName("captcher.duration") val captcherDuration: FiniteDuration,
    val uciMemoTtl: FiniteDuration,
    val pngUrl: String,
    val pngSize: Int
)

final class Env(
    appConfig: Configuration,
    ws: WSClient,
    db: lila.db.Env,
    baseUrl: BaseUrl,
    userRepo: lila.user.UserRepo,
    mongoCache: lila.memo.MongoCache.Builder,
    hub: lila.hub.Env,
    getLightUser: lila.common.LightUser.Getter,
    asyncCache: lila.memo.AsyncCache.Builder,
    settingStore: lila.memo.SettingStore.Builder
)(implicit system: ActorSystem, scheduler: Scheduler) {

  private val config = appConfig.get[GameConfig]("game")(AutoConfig.loader)

  lazy val gameRepo = new GameRepo(db(config.gameColl))

  lazy val pngExport = new PngExport(ws, config.pngUrl, config.pngSize)

  lazy val divider = wire[Divider]

  lazy val cached: Cached = wire[Cached]

  lazy val paginator = wire[PaginatorBuilder]

  lazy val rewind = wire[Rewind]

  lazy val uciMemo = new UciMemo(gameRepo, config.uciMemoTtl)

  lazy val pgnDump = wire[PgnDump]

  lazy val crosstableApi = new CrosstableApi(
    coll = db(config.crosstableColl),
    matchupColl = db(config.matchupColl),
    userRepo = userRepo,
    gameRepo = gameRepo,
    asyncCache = asyncCache
  )

  lazy val playTime = wire[PlayTimeApi]

  lazy val gamesByUsersStream = wire[GamesByUsersStream]

  lazy val bestOpponents = wire[BestOpponents]

  lazy val rematches: Cache[Game.ID, Game.ID] = Scaffeine()
    .expireAfterWrite(3 hour)
    .build[Game.ID, Game.ID]

  lazy val jsonView = new JsonView(rematchOf = rematches.getIfPresent)

  // eargerly load captcher actor
  private val captcher = system.actorOf(Props(new Captcher(gameRepo)), name = config.captcherName)
  scheduler.scheduleWithFixedDelay(config.captcherDuration, config.captcherDuration) {
    () => captcher ! actorApi.NewCaptcha
  }
}
