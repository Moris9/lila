package lila.round

import java.util.ArrayDeque
import scala.collection.JavaConverters._

import org.joda.time.DateTime
import reactivemongo.api.commands.GetLastError
import reactivemongo.bson._

import lila.db.dsl._
import lila.game.Event
import lila.socket.Socket.SocketVersion
import VersionedEvent.EpochSeconds

/**
 * NOT THREAD SAFE
 * Designed for use within a sequential actor (or a Duct)
 */
private final class History(
    load: Fu[List[VersionedEvent]],
    persist: ArrayDeque[VersionedEvent] => Unit,
    withPersistence: Boolean
) {
  import History.Types._

  // TODO: After scala 2.13, use scala's ArrayDeque
  private[this] var events: ArrayDeque[VersionedEvent] = _

  def getVersion: SocketVersion = {
    waitForLoadedEvents
    Option(events.peekFirst).fold(SocketVersion(0))(_.version)
  }

  def versionDebugString: String = {
      waitForLoadedEvents
    Option(events.peekLast).fold("-:-") { h =>
      s"${Option(events.peekFirst).version}:${h.version}@${h.date - nowSeconds}s"
    }
  }

  def getEventsSince(v: SocketVersion, mon: Option[lila.mon.round.history.PlatformHistory]): EventResult = {
    val version = getVersion
    if (v > version) VersionTooHigh
    else if (v == version) UpToDate
    else {
      mon.foreach { m =>
        m.getEventsDelta(version.value - v.value)
        m.getEventsCount()
      }
      // TODO Ensure that version always goes up by 1, and simplify seeks
      val filteredEvents = events.asScala.dropWhile(_.version <= v).toList
      filteredEvents match {
        case e :: _ if e.version == v.inc => Events(filteredEvents)
        case _ =>
          mon.foreach(_.getEventsTooFar())
          InsufficientHistory
      }
    }
  }

  /* if v+1 refers to an old event,
   * then the client probably has skipped events somehow.
   * Log and send new events.
   * None => client is too late, or has greater version than server. Resync.
   * Some(List.empty) => all is good, do nothing
   * Some(List.nonEmpty) => late client, send new events
   *
   * We check the event age because if the client sends a
   * versionCheck ping while the server sends an event,
   * we can get a false positive.
   * */
  def versionCheck(v: SocketVersion): Option[List[VersionedEvent]] =
    getEventsSince(v, none) match {
      case Events(evs) if evs.headOption.exists(_ hasSeconds 10) => Some(evs)
      case Events(_) | UpToDate => Some(Nil)
      case _ => None
    }

  def getRecentEvents(maxEvents: Int): List[VersionedEvent] = {
    waitForLoadedEvents
    events.asScala.takeRight(maxEvents).toList
  }

  def addEvents(xs: List[Event]): List[VersionedEvent] = {
    waitForLoadedEvents
    val date = nowSeconds

    removeTail(History.maxSize - xs.size)
    pruneEvents(date - History.expireAfterSeconds)
    val veBuff = List.newBuilder[VersionedEvent]
    xs.foldLeft(getVersion.inc) {
      case (vnext, e) =>
        val ve = VersionedEvent(e, vnext, date)
        events.addLast(ve)
        veBuff += ve
        vnext.inc
    }
    if (persistenceEnabled) persist(events)
    veBuff.result
  }

  private def removeTail(maxSize: Int) = {
    if (maxSize <= 0) events.clear()
    else {
      var toRemove = events.size - maxSize
      while (toRemove > 0) {
        events.pollFirst
        toRemove -= 1
      }
    }
  }

  private def pruneEvents(minDate: EpochSeconds) = {
    while ({
      val e = events.peekFirst
      (e ne null) && e.date < minDate
    }) events.pollFirst
  }

  private def waitForLoadedEvents: Unit = {
    if (events == null) {
      val evs = load awaitSeconds 3
      events = new ArrayDeque[VersionedEvent]
      evs.foreach(events.add)
    }
  }

  private var persistenceEnabled = withPersistence

  def enablePersistence: Unit = {
    if (!persistenceEnabled) {
      persistenceEnabled = true
      if (events != null) persist(events)
    }
  }
}

private object History {
  object Types {
    sealed trait EventResult
    object VersionTooHigh extends EventResult
    object UpToDate extends EventResult
    object InsufficientHistory extends EventResult
    final case class Events(value: List[VersionedEvent]) extends EventResult
  }

  private final val maxSize = 25
  private final val expireAfterSeconds = 20

  def apply(coll: Coll)(gameId: String, withPersistence: Boolean): History = new History(
    load = serverStarting ?? load(coll, gameId, withPersistence),
    persist = persist(coll, gameId) _,
    withPersistence = withPersistence
  )

  private def serverStarting = !lila.common.PlayApp.startedSinceMinutes(5)

  private def load(coll: Coll, gameId: String, withPersistence: Boolean): Fu[List[VersionedEvent]] =
    coll.byId[Bdoc](gameId).map { doc =>
      ~doc.flatMap(_.getAs[List[VersionedEvent]]("e"))
    } addEffect {
      case events if events.nonEmpty && !withPersistence => coll.remove($id(gameId)).void
      case _ =>
    }

  private def persist(coll: Coll, gameId: String)(vevs: ArrayDeque[VersionedEvent]) =
    if (!vevs.isEmpty) coll.update(
      $doc("_id" -> gameId),
      $doc(
        "$set" -> $doc("e" -> vevs.toArray(Array[VersionedEvent]())),
        "$setOnInsert" -> $doc("d" -> DateTime.now)
      ),
      upsert = true,
      writeConcern = GetLastError.Unacknowledged
    )
}
