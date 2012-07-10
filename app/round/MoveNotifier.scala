package lila
package round

import socket.Fen

import scalaz.effects._
import akka.actor._
import akka.util.duration._
import akka.util.Timeout
import play.api.libs.concurrent._
import play.api.Play.current

final class MoveNotifier(
    siteHubName: String,
    lobbyHubName: String,
    countMove: () ⇒ Unit) {

  lazy val hubRefs = List(siteHubName, lobbyHubName) map { name ⇒
    Akka.system.actorFor("/user/" + name)
  }

  def apply(gameId: String, fen: String) {
    countMove()
    val message = Fen(gameId, fen)
    hubRefs foreach (_ ! message)
  }
}
