package lila.study

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.hub.{ Duct, DuctMap }
import lila.hub.actorApi.HasUserId
import lila.hub.actorApi.map.Ask
import lila.socket.Socket.{ GetVersion, SocketVersion }
import lila.user.User
import makeTimeout.short

final class Env(
    config: Config,
    lightUserApi: lila.user.LightUserApi,
    gamePgnDump: lila.game.PgnDump,
    divider: lila.game.Divider,
    importer: lila.importer.Importer,
    explorerImporter: lila.explorer.ExplorerImporter,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler,
    notifyApi: lila.notify.NotifyApi,
    getPref: User => Fu[lila.pref.Pref],
    getRelation: (User.ID, User.ID) => Fu[Option[lila.relation.Relation]],
    system: ActorSystem,
    hub: lila.hub.Env,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val settings = new {
    val CollectionStudy = config getString "collection.study"
    val CollectionChapter = config getString "collection.chapter"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val ActorName = config getString "actor.name"
    val SequencerTimeout = config duration "sequencer.timeout"
    val NetDomain = config getString "net.domain"
    val NetBaseUrl = config getString "net.base_url"
    val MaxPerPage = config getInt "paginator.max_per_page"
  }
  import settings._

  private val socketHub = new lila.hub.ActorMapNew(
    mkActor = id => new Socket(
      studyId = Study.Id(id),
      jsonView = jsonView,
      studyRepo = studyRepo,
      chapterRepo = chapterRepo,
      lightUser = lightUserApi.async,
      history = new lila.socket.History(ttl = HistoryMessageTtl),
      uidTimeout = UidTimeout,
      lightStudyCache = lightStudyCache
    ),
    accessTimeout = SocketTimeout,
    name = "study.socket",
    system = system
  )

  def version(studyId: Study.Id): Fu[SocketVersion] =
    socketHub.ask[SocketVersion](studyId.value, GetVersion)

  def isConnected(studyId: Study.Id, userId: User.ID): Fu[Boolean] =
    socketHub.ask[Boolean](studyId.value, HasUserId(userId))

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    api = api,
    evalCacheHandler = evalCacheHandler
  )

  lazy val studyRepo = new StudyRepo(coll = db(CollectionStudy))
  lazy val chapterRepo = new ChapterRepo(coll = db(CollectionChapter))

  lazy val jsonView = new JsonView(
    studyRepo,
    lightUserApi.sync
  )

  private lazy val chapterMaker = new ChapterMaker(
    importer = importer,
    pgnFetch = new PgnFetch,
    lightUser = lightUserApi,
    chat = hub.actor.chat,
    domain = NetDomain
  )

  private lazy val explorerGame = new ExplorerGame(
    importer = explorerImporter,
    lightUser = lightUserApi.sync,
    baseUrl = NetBaseUrl
  )

  private lazy val studyMaker = new StudyMaker(
    lightUser = lightUserApi.sync,
    chapterMaker = chapterMaker
  )

  private lazy val studyInvite = new StudyInvite(
    studyRepo = studyRepo,
    notifyApi = notifyApi,
    getRelation = getRelation,
    getPref = getPref
  )

  private lazy val sequencer = new StudySequencer(
    studyRepo,
    chapterRepo,
    sequencers = new DuctMap(
      mkDuct = _ => Duct.extra.lazyPromise(5.seconds.some)(system),
      accessTimeout = SequencerTimeout
    )
  )

  private lazy val serverEvalRequester = new ServerEval.Requester(
    fishnetActor = hub.actor.fishnet,
    chapterRepo = chapterRepo
  )

  lazy val serverEvalMerger = new ServerEval.Merger(
    sequencer = sequencer,
    api = api,
    chapterRepo = chapterRepo,
    socketHub = socketHub,
    divider = divider
  )

  // study actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.analyse.actorApi.StudyAnalysisProgress(analysis, complete) => serverEvalMerger(analysis, complete)
    }
  }), name = ActorName)

  lazy val api = new StudyApi(
    studyRepo = studyRepo,
    chapterRepo = chapterRepo,
    sequencer = sequencer,
    chapterMaker = chapterMaker,
    studyMaker = studyMaker,
    inviter = studyInvite,
    tagsFixer = new ChapterTagsFixer(chapterRepo, gamePgnDump),
    explorerGameHandler = explorerGame,
    lightUser = lightUserApi.sync,
    scheduler = system.scheduler,
    chat = hub.actor.chat,
    bus = system.lilaBus,
    timeline = hub.actor.timeline,
    socketHub = socketHub,
    serverEvalRequester = serverEvalRequester,
    lightStudyCache = lightStudyCache
  )

  lazy val pager = new StudyPager(
    studyRepo = studyRepo,
    chapterRepo = chapterRepo,
    maxPerPage = lila.common.MaxPerPage(MaxPerPage)
  )

  lazy val pgnDump = new PgnDump(
    chapterRepo = chapterRepo,
    gamePgnDump = gamePgnDump,
    lightUser = lightUserApi.sync,
    netBaseUrl = NetBaseUrl
  )

  lazy val lightStudyCache: LightStudyCache = asyncCache.multi(
    name = "study.lightStudyCache",
    f = studyRepo.lightById,
    expireAfter = _.ExpireAfterWrite(20 minutes)
  )

  def cli = new lila.common.Cli {
    def process = {
      case "study" :: "rank" :: "reset" :: Nil => api.resetAllRanks.map { count => s"$count done" }
    }
  }

  system.lilaBus.subscribeFun('gdprErase) {
    case lila.user.User.GDPRErase(user) => api erase user
  }
}

object Env {

  lazy val current: Env = "study" boot new Env(
    config = lila.common.PlayApp loadConfig "study",
    lightUserApi = lila.user.Env.current.lightUserApi,
    gamePgnDump = lila.game.Env.current.pgnDump,
    divider = lila.game.Env.current.divider,
    importer = lila.importer.Env.current.importer,
    explorerImporter = lila.explorer.Env.current.importer,
    evalCacheHandler = lila.evalCache.Env.current.socketHandler,
    notifyApi = lila.notify.Env.current.api,
    getPref = lila.pref.Env.current.api.getPref,
    getRelation = lila.relation.Env.current.api.fetchRelation,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
