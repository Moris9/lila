package lila.notify

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.*
import play.api.Configuration

import lila.common.Bus
import lila.common.config.*

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    getLightUser: lila.common.LightUser.Getter,
    getLightUserSync: lila.common.LightUser.GetterSync,
    cacheApi: lila.memo.CacheApi,
    prefApi: lila.pref.PrefApi,
    subsRepo: lila.relation.SubscriptionRepo
)(using
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
):

  lazy val jsonHandlers = wire[JSONHandlers]

  private lazy val notifyColl = db(CollName("notify"))

  private val maxPerPage = MaxPerPage(7)

  private lazy val repo = wire[NotificationRepo]

  lazy val api = wire[NotifyApi]

  // api actor
  Bus.subscribeFun("notify") {
    case lila.hub.actorApi.notify.NotifiedBatch(userIds) =>
      api.markAllRead(userIds map(UserId apply _)) unit
    case lila.game.actorApi.CorresAlarmEvent(pov) =>
      pov.player.userId ?? { userId =>
        lila.game.Namer.playerText(pov.opponent)(using getLightUser) foreach { opponent =>
          api notifyOne (
            UserId(userId),
            CorresAlarm(
              gameId = pov.gameId,
              opponent = opponent
            )
          )
        }
      }
  }
