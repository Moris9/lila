package lila.challenge

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.hub.TrouperMap
import lila.socket.Socket.{ SocketVersion, GetVersionP }
import lila.user.User

final class Env(
    config: Config,
    system: ActorSystem,
    onStart: String => Unit,
    gameCache: lila.game.Cached,
    lightUser: lila.common.LightUser.GetterSync,
    isOnline: lila.user.User.ID => Boolean,
    hub: lila.hub.Env,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder,
    getPref: User => Fu[lila.pref.Pref],
    getRelation: (User, User) => Fu[Option[lila.relation.Relation]],
    scheduler: lila.common.Scheduler
) {

  private val settings = new {
    val CollectionChallenge = config getString "collection.challenge"
    val MaxPerUser = config getInt "max_per_user"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val MaxPlaying = config getInt "max_playing"
  }
  import settings._

  private val socketMap: SocketMap = new TrouperMap[ChallengeSocket](
    mkTrouper = (challengeId: String) => new ChallengeSocket(
      system = system,
      challengeId = challengeId,
      history = new lila.socket.History(ttl = HistoryMessageTtl),
      getChallenge = repo.byId,
      uidTtl = UidTimeout,
      keepMeAlive = () => socketMap touch challengeId
    ),
    accessTimeout = SocketTimeout
  )
  system.lilaBus.subscribeFun('deploy) { case m => socketMap tellAll m }
  system.scheduler.schedule(30 seconds, 30 seconds) {
    socketMap.monitor("challenge.socketMap")
  }
  system.scheduler.schedule(10 seconds, 3677 millis) {
    socketMap tellAll lila.socket.actorApi.Broom
  }

  def version(challengeId: Challenge.ID): Fu[SocketVersion] =
    socketMap.askIfPresentOrZero[SocketVersion](challengeId)(GetVersionP)

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketMap = socketMap,
    pingChallenge = api.ping
  )

  lazy val api = new ChallengeApi(
    repo = repo,
    joiner = new Joiner(onStart = onStart),
    jsonView = jsonView,
    gameCache = gameCache,
    maxPlaying = MaxPlaying,
    socketMap = socketMap,
    asyncCache = asyncCache,
    lilaBus = system.lilaBus
  )

  lazy val granter = new ChallengeGranter(
    getPref = getPref,
    getRelation = getRelation
  )

  private lazy val repo = new ChallengeRepo(
    coll = db(CollectionChallenge),
    maxPerUser = MaxPerUser
  )

  lazy val jsonView = new JsonView(lightUser, isOnline)

  scheduler.future(3 seconds, "sweep challenges") {
    api.sweep
  }
}

object Env {

  lazy val current: Env = "challenge" boot new Env(
    config = lila.common.PlayApp loadConfig "challenge",
    system = lila.common.PlayApp.system,
    onStart = lila.game.Env.current.onStart,
    hub = lila.hub.Env.current,
    gameCache = lila.game.Env.current.cached,
    lightUser = lila.user.Env.current.lightUserSync,
    isOnline = lila.user.Env.current.isOnline,
    db = lila.db.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache,
    getPref = lila.pref.Env.current.api.getPref,
    getRelation = lila.relation.Env.current.api.fetchRelation,
    scheduler = lila.common.PlayApp.scheduler
  )
}
