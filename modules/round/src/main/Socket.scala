package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import chess.{ Color, White, Black }
import lila.game.Event
import lila.hub.TimeBomb
import lila.socket._
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import makeTimeout.short

private[round] final class Socket(
    gameId: String,
    makeHistory: () ⇒ History,
    getUsername: String ⇒ Fu[Option[String]],
    uidTimeout: Duration,
    socketTimeout: Duration,
    playerTimeout: Duration) extends SocketActor[Member](uidTimeout) {

  private val history = context.actorOf(Props(makeHistory()), name = "history")

  private val timeBomb = new TimeBomb(socketTimeout)

  // when the players have been seen online for the last time
  private var whiteTime = nowMillis
  private var blackTime = nowMillis

  def receiveSpecific = {

    case PingVersion(uid, v) ⇒ {
      timeBomb.delay
      ping(uid)
      ownerOf(uid) foreach { o ⇒
        if (playerIsGone(o.color)) notifyGone(o.color, false)
        playerTime(o.color, nowMillis)
      }
      withMember(uid) { member ⇒
        history ? GetEventsSince(v) foreach {
          case MaybeEvents(events) ⇒ events.fold(resyncNow(member))(batch(member, _))
        }
      }
    }

    case Ack(uid) ⇒ withMember(uid) { _.channel push ackEvent }

    case Broom ⇒ {
      broom
      if (timeBomb.boom) self ! PoisonPill
      else Color.all foreach { c ⇒
        if (playerIsGone(c)) notifyGone(c, true)
      }
    }

    case GetVersion    ⇒ history ? GetVersion pipeTo sender

    case IsGone(color) ⇒ sender ! playerIsGone(color)

    case Join(uid, user, version, color, playerId) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, color, playerId)
      addMember(uid, member)
      notifyCrowd
      if (playerIsGone(color)) notifyGone(color, false)
      playerTime(color, nowMillis)
      sender ! Connected(enumerator, member)
    }

    case Nil            ⇒
    case events: Events ⇒ notify(events)

    case AnalysisAvailable ⇒ {
      notifyAll("analysisAvailable", true)
    }

    case Quit(uid) ⇒ {
      quit(uid)
      notifyCrowd
    }
  }

  def notifyCrowd {
    members.values.filter(_.watcher).map(_.userId).toList.partition(_.isDefined) match {
      case (users, anons) ⇒
        (users.flatten.distinct map getUsername).sequenceFu map { userList ⇒
          notify(Event.Crowd(
            white = ownerOf(White).isDefined,
            black = ownerOf(Black).isDefined,
            watchers = showSpectators(userList.flatten, anons.size)
          ) :: Nil)
        } logFailure ("[round] notify crowd")
    }
  }

  def notify(events: Events) {
    history ? AddEvents(events) mapTo manifest[List[VersionedEvent]] foreach { vevents ⇒
      members.values foreach { m ⇒ batch(m, vevents) }
    }
  }

  def batch(member: Member, vevents: List[VersionedEvent]) {
    if (vevents.nonEmpty) {
      member.channel push makeMessage("b", vevents map (_ jsFor member))
    }
  }

  def notifyOwner[A: Writes](color: Color, t: String, data: A) {
    ownerOf(color) foreach { m ⇒
      m.channel push makeMessage(t, data)
    }
  }

  def notifyGone(color: Color, gone: Boolean) {
    notifyOwner(!color, "gone", gone)
  }

  private val ackEvent = Json.obj("t" -> "ack")

  def ownerOf(color: Color): Option[Member] =
    members.values find { m ⇒ m.owner && m.color == color }

  def ownerOf(uid: String): Option[Member] =
    members get uid filter (_.owner)

  def playerTime(color: Color): Double = color.fold(whiteTime, blackTime)

  def playerTime(color: Color, time: Double) {
    color.fold(whiteTime = time, blackTime = time)
  }

  def playerIsGone(color: Color) =
    playerTime(color) < (nowMillis - playerTimeout.toMillis)
}
