package lila.tournament

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.hub.{ Duct, DuctMap, TrouperMap }
import lila.socket.History
import lila.socket.Socket.{ GetVersionP, SocketVersion }
import lila.user.User
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    flood: lila.security.Flood,
    hub: lila.hub.Env,
    roundMap: DuctMap[_],
    roundSocketHub: ActorSelection,
    lightUserApi: lila.user.LightUserApi,
    isOnline: String => Boolean,
    onStart: String => Unit,
    historyApi: lila.history.HistoryApi,
    trophyApi: lila.user.TrophyApi,
    notifyApi: lila.notify.NotifyApi,
    scheduler: lila.common.Scheduler,
    startedSinceSeconds: Int => Boolean
) {

  private val startsAtMillis = nowMillis

  private val settings = new {
    val CollectionTournament = config getString "collection.tournament"
    val CollectionPlayer = config getString "collection.player"
    val CollectionPairing = config getString "collection.pairing"
    val CollectionLeaderboard = config getString "collection.leaderboard"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val CreatedCacheTtl = config duration "created.cache.ttl"
    val LeaderboardCacheTtl = config duration "leaderboard.cache.ttl"
    val RankingCacheTtl = config duration "ranking.cache.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val ApiActorName = config getString "api_actor.name"
    val SequencerTimeout = config duration "sequencer.timeout"
    val ChannelStanding = config getString "channel.standing.name "
    val NetDomain = config getString "net.domain"
  }
  import settings._

  lazy val forms = new DataForm

  lazy val cached = new Cached(
    asyncCache = asyncCache,
    createdTtl = CreatedCacheTtl,
    rankingTtl = RankingCacheTtl
  )(system)

  lazy val verify = new Condition.Verify(historyApi)

  lazy val winners = new WinnersApi(
    coll = tournamentColl,
    mongoCache = mongoCache,
    ttl = LeaderboardCacheTtl,
    scheduler = scheduler
  )

  lazy val statsApi = new TournamentStatsApi(
    mongoCache = mongoCache
  )

  lazy val shieldApi = new TournamentShieldApi(
    coll = tournamentColl,
    asyncCache = asyncCache
  )

  lazy val revolutionApi = new RevolutionApi(
    coll = tournamentColl,
    asyncCache = asyncCache
  )

  private val duelStore = new DuelStore

  private val pause = new Pause

  lazy val api = new TournamentApi(
    cached = cached,
    scheduleJsonView = scheduleJsonView,
    system = system,
    sequencers = sequencerMap,
    autoPairing = autoPairing,
    clearJsonViewCache = jsonView.clearCache,
    clearWinnersCache = winners.clearCache,
    clearTrophyCache = tour => {
      if (tour.isShield) scheduler.once(10 seconds)(shieldApi.clear)
      else if (Revolution is tour) scheduler.once(10 seconds)(revolutionApi.clear)
    },
    renderer = hub.actor.renderer,
    timeline = hub.actor.timeline,
    socketMap = socketMap,
    trophyApi = trophyApi,
    verify = verify,
    indexLeaderboard = leaderboardIndexer.indexOne _,
    roundMap = roundMap,
    asyncCache = asyncCache,
    duelStore = duelStore,
    pause = pause,
    lightUserApi = lightUserApi
  )

  lazy val crudApi = new crud.CrudApi

  val tourAndRanks = api tourAndRanks _

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketMap = socketMap,
    chat = hub.actor.chat,
    flood = flood
  )

  lazy val jsonView = new JsonView(lightUserApi, cached, statsApi, shieldApi, asyncCache, verify, duelStore, pause, startedSinceSeconds)

  lazy val scheduleJsonView = new ScheduleJsonView(lightUserApi.async)

  lazy val leaderboardApi = new LeaderboardApi(
    coll = leaderboardColl,
    maxPerPage = lila.common.MaxPerPage(15)
  )

  def playerRepo = PlayerRepo

  private lazy val leaderboardIndexer = new LeaderboardIndexer(
    tournamentColl = tournamentColl,
    leaderboardColl = leaderboardColl
  )

  private val socketMap: SocketMap = new TrouperMap[Socket](
    mkTrouper = (tournamentId: String) => new Socket(
      system = system,
      tournamentId = tournamentId,
      history = new History(ttl = HistoryMessageTtl),
      jsonView = jsonView,
      lightUser = lightUserApi.async,
      uidTtl = UidTimeout,
      keepMeAlive = () => socketMap touch tournamentId
    ),
    accessTimeout = SocketTimeout
  )
  system.scheduler.schedule(10 seconds, 10 seconds) {
    socketMap.monitor("tournament.socketMap")
  }
  system.scheduler.schedule(10 seconds, 3819 millis) {
    socketMap tellAll lila.socket.actorApi.Broom
  }

  private val sequencerMap = new DuctMap(
    mkDuct = _ => Duct.extra.lazyFu(5.seconds)(system),
    accessTimeout = SequencerTimeout
  )

  system.lilaBus.subscribe(
    system.actorOf(Props(new ApiActor(api, leaderboardApi, socketMap)), name = ApiActorName),
    'finishGame, 'adjustCheater, 'adjustBooster, 'playban, 'deploy
  )

  system.actorOf(Props(new CreatedOrganizer(
    api = api,
    isOnline = isOnline
  )))

  private val reminder = system.actorOf(Props[Reminder])

  system.actorOf(Props(new StartedOrganizer(
    api = api,
    reminder = reminder,
    isOnline = isOnline,
    socketMap = socketMap
  )))

  TournamentScheduler.start(system, api)

  TournamentInviter.start(system, api, notifyApi)

  def version(tourId: Tournament.ID): Fu[SocketVersion] =
    socketMap.askIfPresentOrZero[SocketVersion](tourId)(GetVersionP.apply)

  // is that user playing a game of this tournament
  // or hanging out in the tournament lobby (joined or not)
  def hasUser(tourId: Tournament.ID, userId: User.ID): Fu[Boolean] =
    socketMap.askIfPresentOrZero[Boolean](tourId)(lila.hub.actorApi.HasUserIdP(userId, _)) >>|
      PairingRepo.isPlaying(tourId, userId)

  def cli = new lila.common.Cli {
    def process = {
      case "tournament" :: "leaderboard" :: "generate" :: Nil =>
        leaderboardIndexer.generateAll inject "Done!"
    }
  }

  private lazy val autoPairing = new AutoPairing(duelStore, onStart)

  private[tournament] lazy val tournamentColl = db(CollectionTournament)
  private[tournament] lazy val pairingColl = db(CollectionPairing)
  private[tournament] lazy val playerColl = db(CollectionPlayer)
  private[tournament] lazy val leaderboardColl = db(CollectionLeaderboard)
}

object Env {

  lazy val current = "tournament" boot new Env(
    config = lila.common.PlayApp loadConfig "tournament",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    mongoCache = lila.memo.Env.current.mongoCache,
    asyncCache = lila.memo.Env.current.asyncCache,
    flood = lila.security.Env.current.flood,
    hub = lila.hub.Env.current,
    roundMap = lila.round.Env.current.roundMap,
    roundSocketHub = lila.hub.Env.current.socket.round,
    lightUserApi = lila.user.Env.current.lightUserApi,
    isOnline = lila.user.Env.current.isOnline,
    onStart = lila.game.Env.current.onStart,
    historyApi = lila.history.Env.current.api,
    trophyApi = lila.user.Env.current.trophyApi,
    notifyApi = lila.notify.Env.current.api,
    scheduler = lila.common.PlayApp.scheduler,
    startedSinceSeconds = lila.common.PlayApp.startedSinceSeconds
  )
}
