package lila.site

import actorApi._
import lila.socket._

private[site] final class SocketHandler(
    socket: akka.actor.ActorRef,
    hub: lila.hub.Env) {

  def apply(
    uid: Socket.Uid,
    userId: Option[String],
    flag: Option[String]): Fu[JsSocketHandler] = {

    Handler(hub, socket, uid, Join(uid, userId, flag)) {
      case Connected(enum, member) => (Handler.emptyController, enum, member)
    }
  }
}
