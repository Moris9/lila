package lila
package lobby

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import scalaz.effects._

import implicits.RichJs._
import socket.{ Util, PingVersion, Quit, LiveGames }
import timeline.Entry
import game.DbGame
import security.Flood

final class Socket(hub: ActorRef, flood: Flood) {

  implicit val timeout = Timeout(1 second)

  def addEntry(entry: Entry): IO[Unit] = io { hub ! AddEntry(entry) }

  def removeHook(hook: Hook): IO[Unit] = io { hub ! RemoveHook(hook) }

  def addHook(hook: Hook): IO[Unit] = io { hub ! AddHook(hook) }

  def biteHook(hook: Hook, game: DbGame): IO[Unit] = io { hub ! BiteHook(hook, game) }

  def reloadTournaments(html: String): IO[Unit] = io { hub ! ReloadTournaments(html) }

  def sysTalk(text: String): IO[Unit] = io { hub ! SysTalk(text) }

  def unTalk(regex: util.matching.Regex): IO[Unit] = io { hub ! UnTalk(regex) }

  def join(
    uidOption: Option[String],
    username: Option[String],
    versionOption: Option[Int],
    hook: Option[String]): SocketPromise = {
    val promise = for {
      version ← versionOption
      uid ← uidOption
    } yield (hub ? Join(uid, username, version, hook)) map {
      case Connected(enumerator, channel) ⇒
        val iteratee = Iteratee.foreach[JsValue] { e ⇒
          e str "t" match {
            case Some("talk") ⇒ for {
              data ← e obj "d"
              txt ← data str "txt"
              if flood.allowMessage(uid, txt)
              uname ← username
            } hub ! Talk(uname, txt)
            case Some("p") ⇒ e int "v" foreach { v ⇒
              hub ! PingVersion(uid, v)
            }
            case Some("liveGames") ⇒ e str "d" foreach { ids ⇒
              hub ! LiveGames(uid, ids.split(' ').toList)
            }
            case _ ⇒
          }
        } mapDone { _ ⇒
          hub ! Quit(uid)
        }
        (iteratee, enumerator)
    }: SocketPromise
    promise | Util.connectionFail
  }
}
