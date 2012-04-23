package lila
package game

import model._
import socket._
import chess.{ Color, White, Black }

import akka.actor._
import akka.util.duration._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.Play.current
import scalaz.effects._

final class Hub(
    gameId: String,
    history: History,
    uidTimeout: Int,
    hubTimeout: Int) extends HubActor[Member](uidTimeout) {

  var lastPingTime = nowMillis

  def receiveSpecific = {

    case Ping(uid) ⇒ {
      ping(uid)
      lastPingTime = nowMillis
    }

    case Broom ⇒ {
      broom()
      if (lastPingTime < (nowMillis - hubTimeout)) {
        context.parent ! CloseGame(gameId)
      }
    }

    case GetGameVersion(_)           ⇒ sender ! history.version

    case IsConnectedOnGame(_, color) ⇒ sender ! member(color).isDefined

    case Join(uid, version, color, owner) ⇒ {
      log.warning("join " + gameId)
      val msgs = history since version filter (_.visible(color, owner)) map (_.js)
      val channel = new LilaEnumerator[JsValue](msgs)
      val member = Member(channel, PovRef(gameId, color), owner)
      members = members + (uid -> member)
      sender ! Connected(member)
      notify(crowdEvent)
    }

    case Events(events) ⇒ events match {
      case Nil           ⇒
      case single :: Nil ⇒ notify(single)
      case multi         ⇒ notify(multi)
    }

    case Quit(uid) ⇒ {
      members = members - uid
      notify(crowdEvent)
    }

    case Close ⇒ {
      members.values foreach { _.channel.close() }
      self ! PoisonPill
    }
  }

  private def crowdEvent = CrowdEvent(
    white = member(White).isDefined,
    black = member(Black).isDefined,
    watchers = members.values count (_.watcher))

  private def notify(e: Event) {
    val vevent = history += e
    members.values filter vevent.visible foreach (_.channel push vevent.js)
  }
  private def notify(events: List[Event]) {
    val vevents = events map history.+=
    members.values foreach { member ⇒
      member.channel push JsObject(Seq(
        "t" -> JsString("batch"),
        "d" -> JsArray(vevents filter (_ visible member) map (_.js))
      ))
    }
  }

  private def member(color: Color): Option[Member] =
    members.values find { m ⇒ m.owner && m.color == color }
}
