package lila.socket

import play.api.libs.json._
import redis.clients.jedis._
import scala.concurrent.Future

import lila.hub.actorApi.round.{ MoveEvent, FinishGameId }
import lila.hub.actorApi.socket.{ SendTo, SendTos, WithUserIds }
import lila.hub.actorApi.{ Deploy, Announce }

private final class RemoteSocket(
    makeRedis: () => Jedis,
    chanIn: String,
    chanOut: String,
    lifecycle: play.api.inject.ApplicationLifecycle,
    notificationActor: akka.actor.ActorSelection,
    bus: lila.common.Bus
) {

  private object In {
    val Connect = "connect"
    val Disconnect = "disconnect"
    val Watch = "watch"
    val Notified = "notified"
  }
  private object Out {
    val Move = "move"
    val TellUser = "tell/user"
    val TellUsers = "tell/users"
    val TellAll = "tell/all"
  }

  private val clientIn = makeRedis()
  private val clientOut = makeRedis()

  private val connectedUserIds = collection.mutable.Set.empty[String]
  private val watchedGameIds = collection.mutable.Set.empty[String]

  bus.subscribeFun('moveEvent, 'finishGameId, 'socketUsers, 'shutdown, 'announce) {
    case MoveEvent(gameId, fen, move) if watchedGameIds(gameId) => send(Out.Move, Json.obj(
      "gameId" -> gameId,
      "fen" -> fen,
      "move" -> move
    ))
    case FinishGameId(gameId) if watchedGameIds(gameId) => watchedGameIds -= gameId
    case SendTos(userIds, payload) =>
      val connectedUsers = userIds intersect connectedUserIds
      if (connectedUsers.nonEmpty) send(Out.TellUsers, Json.obj(
        "users" -> connectedUsers,
        "payload" -> payload
      ))
    case SendTo(userId, payload) if connectedUserIds(userId) => send(Out.TellUser, Json.obj(
      "user" -> userId,
      "payload" -> payload
    ))
    case d: Deploy => send(Out.TellAll, Json.obj(
      "payload" -> Json.obj("t" -> d.key)
    ))
    case Announce(msg) => send(Out.TellAll, Json.obj(
      "payload" -> Json.obj(
        "t" -> "announce",
        "d" -> Json.obj("msg" -> msg)
      )
    ))
    case WithUserIds(f) => f(connectedUserIds)
  }

  private def onReceive(path: String, data: JsObject) = path match {
    case In.Connect => data str "user" foreach { userId =>
      connectedUserIds += userId
      bus.publish(lila.hub.actorApi.relation.ReloadOnlineFriends(userId), 'reloadOnlineFriends)
    }
    case In.Disconnect => data str "user" foreach { userId =>
      connectedUserIds -= userId
    }
    case In.Watch => data str "game" foreach { gameId =>
      watchedGameIds += gameId
    }
    case In.Notified => data str "user" foreach { userId =>
      notificationActor ! lila.hub.actorApi.notify.Notified(userId)
    }
    case path => logger.warn(s"Invalid path $path")
  }

  private def send(path: String, data: JsObject) = clientOut.publish(
    chanOut,
    Json stringify {
      Json.obj("path" -> path) ++ data
    }
  )

  Future {
    clientIn.subscribe(new JedisPubSub() {
      override def onMessage(channel: String, message: String): Unit = {
        try {
          Json.parse(message) match {
            case o: JsObject => o str "path" foreach { onReceive(_, o) }
            case _ => logger warn s"Invalid message $message"
          }
        } catch {
          case _: Exception => logger.warn(s"Can't parse remote socket message $message")
        }
      }
    }, chanIn)
  }

  lifecycle.addStopHook { () =>
    logger.info("Stopping the Redis clients...")
    Future {
      clientIn.quit()
      clientOut.quit()
    }
  }
}
