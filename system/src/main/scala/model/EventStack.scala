package lila.system
package model

import lila.chess._

case class EventStack(events: List[(Int, Event)]) {

  lazy val sortedEvents = events sortBy (_._1)

  lazy val firstVersion: Option[Int] = sortedEvents.headOption map (_._1)

  lazy val lastVersion: Option[Int] = sortedEvents.lastOption map (_._1)

  def encode: String = events map {
    case (version, event) ⇒ version.toString + event.encode
  } mkString "|"

  // Here I found the mutable approach easier
  // I'm probably just missing something.
  // Like the state monad.
  def optimize: EventStack = {
    var previous: Boolean = false
    EventStack(
      (events.reverse take EventStack.maxEvents map {
        case (v, PossibleMovesEvent(_)) if previous ⇒ (v, PossibleMovesEvent(Map.empty))
        case (v, e @ PossibleMovesEvent(_))         ⇒ previous = true; (v, e)
        case x                                      ⇒ x
      }).reverse
    )
  }

  def version: Int = events.lastOption map (_._1) getOrElse 0

  def eventsSince(version: Int): Option[List[Event]] = for {
    first <- firstVersion
    if version >= first - 1
    last <- lastVersion
    if version <= last
  } yield sortedEvents dropWhile { ve => ve._1 <= version } map (_._2)

  def withEvents(newEvents: List[Event]): EventStack = {

    def versionEvents(v: Int, events: List[Event]): List[(Int, Event)] = events match {
      case Nil           ⇒ Nil
      case event :: rest ⇒ (v + 1, event) :: versionEvents(v + 1, rest)
    }

    copy(events = events ++ versionEvents(version, newEvents))
  }
}

object EventStack {

  val maxEvents = 16

  val EventEncoding = """^(\d+)(\w)(.*)$""".r

  def decode(evts: String): EventStack = new EventStack(
    (evts.split('|') collect {
      case EventEncoding(v, code, data) ⇒ for {
        version ← parseIntOption(v)
        decoder ← EventDecoder.all get code(0)
        event ← decoder decode data
      } yield (version, event)
    }).toList.flatten
  )

  def apply(): EventStack = new EventStack(Nil)

  def build(events: Event*): EventStack =
    new EventStack(events.zipWithIndex map (_.swap) toList)
}
