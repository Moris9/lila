package lila
package socket

case object Close
case object GetNbMembers
case class Ping(uid: String)
case object Cleanup
