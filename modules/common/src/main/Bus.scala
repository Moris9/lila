package lila.common

import scala.concurrent.duration._
import scala.concurrent.Promise
import scala.jdk.CollectionConverters._

import akka.actor.{ ActorRef, ActorSystem }

object Bus {

  case class Event(payload: Any, channel: String)
  type Channel    = String
  type Subscriber = Tellable

  def publish(payload: Any, channel: Channel): Unit = bus.publish(payload, channel)

  def subscribe = bus.subscribe _

  def subscribe(ref: ActorRef, to: Channel) = bus.subscribe(Tellable(ref), to)

  def subscribe(subscriber: Tellable, to: Channel*)   = to foreach { bus.subscribe(subscriber, _) }
  def subscribe(ref: ActorRef, to: Channel*)          = to foreach { bus.subscribe(Tellable(ref), _) }
  def subscribe(ref: ActorRef, to: Iterable[Channel]) = to foreach { bus.subscribe(Tellable(ref), _) }

  def subscribeFun(to: Channel*)(f: PartialFunction[Any, Unit]): Tellable = {
    val t = lila.common.Tellable(f)
    subscribe(t, to: _*)
    t
  }

  def subscribeFuns(subscriptions: (Channel, PartialFunction[Any, Unit])*): Unit =
    subscriptions foreach {
      case (channel, subscriber) => subscribeFun(channel)(subscriber)
    }

  def unsubscribe                               = bus.unsubscribe _
  def unsubscribe(ref: ActorRef, from: Channel) = bus.unsubscribe(Tellable(ref), from)

  def unsubscribe(subscriber: Tellable, from: Iterable[Channel]) = from foreach {
    bus.unsubscribe(subscriber, _)
  }
  def unsubscribe(ref: ActorRef, from: Iterable[Channel]) = from foreach { bus.unsubscribe(Tellable(ref), _) }

  def ask[A](channel: Channel, timeout: FiniteDuration = 1.second)(makeMsg: Promise[A] => Any)(
      implicit
      ec: scala.concurrent.ExecutionContext,
      system: ActorSystem
  ): Fu[A] = {
    val promise = Promise[A]
    val msg     = makeMsg(promise)
    publish(msg, channel)
    promise.future.withTimeout(
      timeout,
      Bus.AskTimeout(s"Bus.ask timeout: $channel $msg")
    )
  }

  private val bus = new EventBus[Any, Channel, Tellable](
    initialCapacity = 65535,
    publish = (tellable, event) => tellable ! event
  )

  def keys      = bus.keys
  def size      = bus.size
  def destroy() = bus.destroy

  case class AskTimeout(message: String) extends lila.base.LilaException
}

final private class EventBus[Event, Channel, Subscriber](
    initialCapacity: Int,
    publish: (Subscriber, Event) => Unit
) {

  import java.util.concurrent.ConcurrentHashMap

  private val entries = new ConcurrentHashMap[Channel, Set[Subscriber]](initialCapacity)
  private var alive   = true

  def subscribe(subscriber: Subscriber, channel: Channel): Unit =
    if (alive)
      entries.compute(channel, (_: Channel, subs: Set[Subscriber]) => {
        Option(subs).fold(Set(subscriber))(_ + subscriber)
      })

  def unsubscribe(subscriber: Subscriber, channel: Channel): Unit =
    if (alive)
      entries.computeIfPresent(channel, (_: Channel, subs: Set[Subscriber]) => {
        val newSubs = subs - subscriber
        if (newSubs.isEmpty) null
        else newSubs
      })

  def publish(event: Event, channel: Channel): Unit =
    Option(entries get channel) foreach {
      _ foreach {
        publish(_, event)
      }
    }

  def keys: Set[Channel]       = entries.keySet.asScala.toSet
  def size                     = entries.size
  def sizeOf(channel: Channel) = Option(entries get channel).fold(0)(_.size)
  def destroy() = {
    alive = false
    entries.clear()
  }
}
