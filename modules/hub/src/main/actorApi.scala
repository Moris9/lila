package lila.hub
package actorApi

import play.api.libs.json._
import akka.actor.ActorRef

case class SendTo(userId: String, message: JsObject)

object SendTo {
  def apply[A: Writes](userId: String, typ: String, data: A): SendTo =
    SendTo(userId, Json.obj("t" -> typ, "d" -> data))
}

case class SendTos(userIds: Set[String], message: JsObject)

object SendTos {
  def apply[A: Writes](userIds: Set[String], typ: String, data: A): SendTos =
    SendTos(userIds, Json.obj("t" -> typ, "d" -> data))
}

sealed abstract class RemindDeploy(val key: String)
case object RemindDeployPre extends RemindDeploy("deployPre")
case object RemindDeployPost extends RemindDeploy("deployPost")
case class Deploy(event: RemindDeploy, html: String)

package map {
case class Get(id: String)
case class Tell(id: String, msg: Any)
case class TellAll(msg: Any)
case class Ask(id: String, msg: Any)
case object Size
}

case class WithUserIds(f: Iterable[String] ⇒ Unit)

case object GetUids

package chat {
case class Input(uid: String, json: JsObject)
case class System(chanTyp: String, chanId: Option[String], text: String)
}

package setup {
case class RemindChallenge(gameId: String, from: String, to: String)
case class DeclineChallenge(gameId: String)
}

package captcha {
case object AnyCaptcha
case class GetCaptcha(id: String)
case class ValidCaptcha(id: String, solution: String)
}

package lobby {
case class ReloadTournaments(html: String)
case object NewForumPost
}

package timeline {
case class ReloadTimeline(user: String)

sealed trait Atom
case class Follow(u1: String, u2: String) extends Atom
case class TeamJoin(userId: String, teamId: String) extends Atom
case class TeamCreate(userId: String, teamId: String) extends Atom
case class ForumPost(userId: String, topicName: String, postId: String) extends Atom

object atomFormat {

  implicit val followFormat = Json.format[Follow]
  implicit val teamJoinFormat = Json.format[TeamJoin]
  implicit val teamCreateFormat = Json.format[TeamCreate]
  implicit val forumPostFormat = Json.format[ForumPost]
}

object propagation {
  sealed trait Propagation
  case class Users(users: List[String]) extends Propagation
  case class Friends(user: String) extends Propagation
  case class StaffFriends(user: String) extends Propagation
}

import propagation._

case class Propagate(data: Atom, propagations: List[Propagation] = Nil) {
  def toUsers(ids: List[String]) = copy(propagations = Users(ids) :: propagations)
  def toFriendsOf(id: String) = copy(propagations = Friends(id) :: propagations)
  def toStaffFriendsOf(id: String) = copy(propagations = StaffFriends(id) :: propagations)
}
}

package game {
case object Count
}

package message {
case class LichessThread(to: String, subject: String, message: String)
}

package router {
case class Abs(route: Any)
case class Nolang(route: Any)
case object Homepage
case class TeamShow(id: String)
case class User(username: String)
case class Player(fullId: String)
case class Watcher(gameId: String, color: String)
case class Replay(gameId: String, color: String)
case class Pgn(gameId: String)
case class Tourney(tourId: String)
}

package forum {
case class MakeTeam(id: String, name: String)
}

package ai {
case object GetLoad
case class Analyse(uciMoves: List[String], initialFen: Option[String])
}

package monitor {
case object AddRequest
case object Update
}

package round {
case class MoveEvent(
  gameId: String,
  fen: String,
  move: String,
  ip: String,
  meta: String) // x, +, #, +x, #x
case class FinishGame(gameId: String)
}

package bookmark {
case class Toggle(gameId: String, userId: String)
case class Remove(gameIds: List[String])
}

package relation {
case class ReloadOnlineFriends(userId: String)
case class GetOnlineFriends(userId: String)
case class OnlineFriends(usernames: List[String], nb: Int)
case class Block(u1: String, u2: String)
case class UnBlock(u1: String, u2: String)
}
