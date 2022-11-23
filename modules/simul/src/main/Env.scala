package lila.simul

import com.softwaremill.macwire.*
import play.api.Configuration
import scala.concurrent.duration.*

import lila.common.autoconfig.{ *, given }
import lila.common.Bus
import lila.common.config.*
import lila.socket.{ GetVersion, SocketVersion }

@Module
private class SimulConfig(
    @ConfigName("collection.simul") val simulColl: CollName,
    @ConfigName("feature.views") val featureViews: Max
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    renderer: lila.hub.actors.Renderer,
    timeline: lila.hub.actors.Timeline,
    chatApi: lila.chat.ChatApi,
    lightUser: lila.common.LightUser.Getter,
    onGameStart: lila.round.OnStart,
    cacheApi: lila.memo.CacheApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    proxyRepo: lila.round.GameProxyRepo,
    isOnline: lila.socket.IsOnline
)(using
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler,
    mode: play.api.Mode
):

  private val config = appConfig.get[SimulConfig]("simul")(AutoConfig.loader)

  private lazy val simulColl = db(config.simulColl)

  lazy val repo: SimulRepo = wire[SimulRepo]

  lazy val api: SimulApi = wire[SimulApi]

  lazy val jsonView = wire[JsonView]

  private val simulSocket = wire[SimulSocket]

  val isHosting = new lila.round.IsSimulHost(u => api.currentHostIds dmap (_ contains u))

  val allCreatedFeaturable = cacheApi.unit[List[Simul]] {
    _.refreshAfterWrite(3 seconds)
      .buildAsyncFuture(_ => repo.allCreatedFeaturable)
  }

  val featurable = new SimulIsFeaturable((simul: Simul) =>
    simul.team.isEmpty && featureLimiter(simul.hostId)(true)(false)
  )

  private val featureLimiter = new lila.memo.RateLimit[lila.user.User.ID](
    credits = config.featureViews.value,
    duration = 24 hours,
    key = "simul.feature",
    log = false
  )

  def version(simulId: SimulId) =
    simulSocket.rooms.ask[SocketVersion](simulId into RoomId)(GetVersion.apply)

  Bus.subscribeFuns(
    "finishGame" -> { case lila.game.actorApi.FinishGame(game, _, _) =>
      api finishGame game
      ()
    },
    "adjustCheater" -> { case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      api ejectCheater userId
      ()
    },
    "simulGetHosts" -> { case lila.hub.actorApi.simul.GetHostIds(promise) =>
      promise completeWith api.currentHostIds
    },
    "moveEventSimul" -> { case lila.hub.actorApi.round.SimulMoveEvent(move, _, opponentUserId) =>
      import lila.common.Json.given
      Bus.publish(
        lila.hub.actorApi.socket.SendTo(
          opponentUserId,
          lila.socket.Socket.makeMessage("simulPlayerMove", move.gameId)
        ),
        "socketUsers"
      )
    }
  )

final class SimulIsFeaturable(f: Simul => Boolean) extends (Simul => Boolean):
  def apply(simul: Simul) = f(simul)
