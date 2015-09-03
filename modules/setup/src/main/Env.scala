package lila.setup

import akka.actor._
import com.typesafe.config.{ Config => AppConfig }

import lila.common.PimpedConfig._
import lila.game.{ Game, Pov, Progress }
import lila.user.UserContext

final class Env(
    config: AppConfig,
    db: lila.db.Env,
    hub: lila.hub.Env,
    onStart: String => Unit,
    aiPlay: Game => Fu[Progress],
    system: ActorSystem) {

  private val FriendMemoTtl = config duration "friend.memo.ttl"
  private val CollectionUserConfig = config getString "collection.user_config"
  private val CollectionAnonConfig = config getString "collection.anon_config"
  private val ChallengerName = config getString "challenger.name"

  val CasualOnly = config getBoolean "casual_only"

  lazy val forms = new FormFactory(CasualOnly)

  def filter(ctx: UserContext): Fu[FilterConfig] =
    ctx.me.fold(AnonConfigRepo filter ctx.req)(UserConfigRepo.filter)

  lazy val processor = new Processor(
    lobby = hub.actor.lobby,
    friendConfigMemo = friendConfigMemo,
    router = hub.actor.router,
    onStart = onStart,
    aiPlay = aiPlay)

  lazy val friendJoiner = new FriendJoiner(
    friendConfigMemo = friendConfigMemo,
    onStart = onStart)

  lazy val friendConfigMemo = new FriendConfigMemo(ttl = FriendMemoTtl)

  system.actorOf(Props(new Challenger(
    roundHub = hub.socket.round,
    renderer = hub.actor.renderer
  )), name = ChallengerName)

  private[setup] lazy val userConfigColl = db(CollectionUserConfig)
  private[setup] lazy val anonConfigColl = db(CollectionAnonConfig)
}

object Env {

  lazy val current = "setup" boot new Env(
    config = lila.common.PlayApp loadConfig "setup",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    onStart = lila.game.Env.current.onStart,
    aiPlay = lila.round.Env.current.aiPlay,
    system = lila.common.PlayApp.system)
}
