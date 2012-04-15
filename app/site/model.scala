package lila
package site

import scalaz.effects.IO

case class Member(
    channel: Channel,
    username: Option[String]) {
}

case class Join(
    uid: String,
    username: Option[String])
case class Quit(uid: String)
case class Connected(channel: Channel)
case object GetUsernames
case object NbMembers
