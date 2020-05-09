package lila.hub

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import scala.collection.immutable.Queue
import scala.concurrent.Promise

/*
 * Sequential like an actor, but for async functions,
 * and using an atomic backend instead of akka actor.
 */
abstract class Duct(implicit ec: scala.concurrent.ExecutionContext) extends lila.common.Tellable {

  import Duct._

  // implement async behaviour here
  protected val process: ReceiveAsync

  def !(msg: Any): Unit =
    if (stateRef.getAndUpdate(state => Some(state.fold(emptyQueue)(_ enqueue msg))).isEmpty) run(msg)

  def ask[A](makeMsg: Promise[A] => Any): Fu[A] = {
    val promise = Promise[A]
    this ! makeMsg(promise)
    promise.future
  }

  def queueSize = stateRef.get().fold(0)(_.size + 1)

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private[this] val stateRef: AtomicReference[State] = new AtomicReference(None)

  private[this] def run(msg: Any): Unit =
    process.applyOrElse(msg, Duct.fallback) onComplete postRun

  private[this] val postRun = (_: Any) =>
    stateRef.getAndUpdate(postRunUpdate) flatMap (_.headOption) foreach run
}

object Duct {

  type ReceiveAsync = PartialFunction[Any, Fu[Any]]

  case class SizedQueue(queue: Queue[Any], size: Int) {
    def enqueue(a: Any) = SizedQueue(queue enqueue a, size + 1)
    def isEmpty         = size == 0
    def tailOption      = !isEmpty option SizedQueue(queue.tail, size - 1)
    def headOption      = queue.headOption
  }
  val emptyQueue = SizedQueue(Queue.empty, 0)

  private type State = Option[SizedQueue]

  private val postRunUpdate = new UnaryOperator[State] {
    override def apply(state: State): State = state.flatMap(_.tailOption)
  }

  private val fallback = { msg: Any =>
    lila.log("Duct").warn(s"unhandled msg: $msg")
    funit
  }
}
