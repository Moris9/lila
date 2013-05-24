package lila.site
package actorApi

import play.api.libs.json._

import lila.socket.SocketMember

case class Member(
  channel: JsChannel,
  userId: Option[String],
  flag: Option[String]) extends SocketMember {

  def hasFlag(f: String) = flag ?? (f ==)
}

case class Join(uid: String, userId: Option[String], flag: Option[String])
