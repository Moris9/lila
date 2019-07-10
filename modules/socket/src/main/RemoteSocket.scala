package lila.socket

import io.lettuce.core._
import play.api.libs.json._
import scala.concurrent.Future

import chess.Centis
import lila.common.{ Chronometer, WithResource }
import lila.hub.actorApi.round.{ MoveEvent, FinishGameId, Mlat }
import lila.hub.actorApi.socket.{ SendTo, SendTos, WithUserIds }
import lila.hub.actorApi.{ Deploy, Announce }

private final class RemoteSocket(
    redisClient: RedisClient,
    chanIn: String,
    chanOut: String,
    lifecycle: play.api.inject.ApplicationLifecycle,
    notificationActor: akka.actor.ActorSelection,
    setNb: Int => Unit,
    bus: lila.common.Bus
) {

  private object In {
    val Connect = "connect"
    val Disconnect = "disconnect"
    val DisconnectAll = "disconnect/all"
    val Watch = "watch"
    val Notified = "notified"
    val Connections = "connections"
    val Lag = "lag"
  }
  private object Out {
    val Move = "move"
    val TellUser = "tell/user"
    val TellUsers = "tell/users"
    val TellAll = "tell/all"
    val TellFlag = "tell/flag"
    val Mlat = "mlat"
  }

  private val connectedUserIds = collection.mutable.Set.empty[String]
  private val watchedGameIds = collection.mutable.Set.empty[String]

  bus.subscribeFun('moveEvent, 'finishGameId, 'socketUsers, 'deploy, 'announce, 'mlat, 'sendToFlag) {
    case MoveEvent(gameId, fen, move) =>
      if (watchedGameIds(gameId)) send(Out.Move, gameId, move, fen)
    case FinishGameId(gameId) if watchedGameIds(gameId) =>
      watchedGameIds -= gameId
    case SendTos(userIds, payload) =>
      val connectedUsers = userIds intersect connectedUserIds
      if (connectedUsers.nonEmpty) send(Out.TellUsers, connectedUsers mkString ",", Json stringify payload)
    case SendTo(userId, payload) if connectedUserIds(userId) =>
      send(Out.TellUser, userId, Json stringify payload)
    case d: Deploy =>
      send(Out.TellAll, Json stringify Json.obj("t" -> d.key))
    case Announce(msg) =>
      send(Out.TellAll, Json stringify Json.obj("t" -> "announce", "d" -> Json.obj("msg" -> msg)))
    case Mlat(ms) =>
      send(Out.Mlat, ms.toString)
    case actorApi.SendToFlag(flag, payload) =>
      send(Out.TellFlag, flag, Json stringify payload)
    case WithUserIds(f) =>
      f(connectedUserIds)
  }

  private def onReceive(path: String, args: String) = path match {
    case In.Connect =>
      val userId = args
      connectedUserIds += userId
    case In.Disconnect =>
      val userId = args
      connectedUserIds -= args
    case In.DisconnectAll =>
      logger.info("Remote socket disconnect all")
      connectedUserIds.clear
      watchedGameIds.clear
    case In.Watch =>
      val gameId = args
      watchedGameIds += gameId
    case In.Notified =>
      val userId = args
      notificationActor ! lila.hub.actorApi.notify.Notified(userId)
    case In.Connections =>
      parseIntOption(args) foreach { nb =>
        setNb(nb)
        tick(nb)
      }
    case In.Lag => args split ' ' match {
      case Array(user, l) => parseIntOption(l) foreach { lag =>
        UserLagCache.put(user, Centis(lag))
      }
      case _ =>
    }
    case path =>
      logger.warn(s"Invalid path $path")
  }

  private def send(path: String, args: String*): Unit = {
    val chrono = Chronometer.start
    Chronometer.syncMon(_.socket.remote.redis.publishTimeSync) {
      connOut.async.publish(chanOut, s"$path ${args mkString " "}").thenRun {
        new Runnable { def run = chrono.mon(_.socket.remote.redis.publishTime) }
      }
      // .mon(_.socket.remote.redis.publishTime)
      // .logFailure(logger)
    }
    redisMon.out()
  }

  private def tick(nbConn: Int): Unit = {
    mon.connections(nbConn)
    mon.sets.users(connectedUserIds.size)
    mon.sets.games(watchedGameIds.size)
  }

  private val connIn = redisClient.connectPubSub()
  private val connOut = redisClient.connectPubSub()
  private val mon = lila.mon.socket.remote
  private val redisMon = mon.redis

  connIn.addListener(new pubsub.RedisPubSubAdapter[String, String] {
    override def message(channel: String, message: String): Unit = {
      val parts = message.split(" ", 2)
      onReceive(parts(0), ~parts.lift(1))
      redisMon.in()
    }
  })
  connIn.async.subscribe(chanIn)

  lifecycle.addStopHook { () =>
    logger.info("Stopping the Redis pool...")
    Future {
      connIn.close();
      connOut.close();
      redisClient.shutdown();
    }
  }
}
