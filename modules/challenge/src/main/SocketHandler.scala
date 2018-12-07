package lila.challenge

import akka.actor._
import akka.pattern.ask

import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.socket.Socket.{ Uid, SocketVersion }
import lila.user.User
import makeTimeout.short

private[challenge] final class SocketHandler(
    hub: lila.hub.Env,
    socketMap: SocketMap,
    pingChallenge: Challenge.ID => Funit
) {

  import ChallengeSocket._

  def join(
    challengeId: Challenge.ID,
    uid: Uid,
    userId: Option[User.ID],
    owner: Boolean,
    version: Option[SocketVersion]
  ): Fu[JsSocketHandler] = {
    val socket = socketMap getOrMake challengeId
    socket.ask[Connected](JoinP(uid, userId, owner, version, _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        controller(socket, challengeId, uid, member),
        member,
        socket,
        uid
      ) -> enum
    }
  }

  private def controller(
    socket: ChallengeSocket,
    challengeId: Challenge.ID,
    uid: Uid,
    member: Member
  ): Handler.Controller = {
    case ("p", o) => socket ! Ping(uid, o)
    case ("ping", _) if member.owner => pingChallenge(challengeId)
  }
}
