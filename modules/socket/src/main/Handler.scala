package lila.socket

import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.common.PimpedJson._
import lila.hub.actorApi.relation.ReloadOnlineFriends
import makeTimeout.large

object Handler {

  type Controller = PartialFunction[(String, JsObject), Unit]
  type Connecter = PartialFunction[Any, (Controller, JsEnumerator, SocketMember)]

  val emptyController: Controller = PartialFunction.empty

  def apply(
    hub: lila.hub.Env,
    socket: ActorRef,
    uid: String,
    join: Any,
    userId: Option[String])(connecter: Connecter): Fu[JsSocketHandler] = {

    lazy val AnaRateLimit = new lila.memo.RateLimitGlobal(1 / 3 second)

    def baseController(member: SocketMember): Controller = {
      case ("p", _) => socket ! Ping(uid)
      case ("following_onlines", _) => userId foreach { u =>
        hub.actor.relation ! ReloadOnlineFriends(u)
      }
      case ("startWatching", o) => o str "d" foreach { ids =>
        hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
      }
      case ("moveLat", o) => hub.channel.moveLat ! (~(o boolean "d")).fold(
        Channel.Sub(member),
        Channel.UnSub(member))
      case ("anaMove", o) => AnaRateLimit {
        AnaMove parse o foreach { anaMove =>
          anaMove.step match {
            case scalaz.Success(step) =>
              member push lila.socket.Socket.makeMessage("step", Json.obj(
                "step" -> step.toJson,
                "path" -> anaMove.path
              ))
            case scalaz.Failure(err) =>
              member push lila.socket.Socket.makeMessage("stepFailure", err.toString)
          }
        }
      }
      case ("anaDests", o) => AnaRateLimit {
        AnaDests parse o match {
          case Some(req) =>
            member push lila.socket.Socket.makeMessage("dests", Json.obj(
              "dests" -> req.dests,
              "path" -> req.path
            ))
          case None =>
            member push lila.socket.Socket.makeMessage("destsFailure", "Bad dests request")
        }
      }
      case _ => // logwarn("Unhandled msg: " + msg)
    }

    def iteratee(controller: Controller, member: SocketMember): JsIteratee = {
      val control = controller orElse baseController(member)
      Iteratee.foreach[JsValue](jsv =>
        jsv.asOpt[JsObject] foreach { obj =>
          obj str "t" foreach { t =>
            control.lift(t -> obj)
          }
        }
      ).map(_ => socket ! Quit(uid))
    }

    socket ? join map connecter map {
      case (controller, enum, member) => iteratee(controller, member) -> enum
    }
  }
}
