package lila.hub

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Promise

final class FutureSequencer(
    system: ActorSystem,
    receiveTimeout: Option[FiniteDuration],
    executionTimeout: Option[FiniteDuration] = None) {

  import FutureSequencer._

  private val sequencer =
    system.actorOf(Props(classOf[FSequencer], receiveTimeout, executionTimeout))

  def apply[A: Manifest](op: => Fu[A]): Fu[A] = {
    val promise = Promise[A]()
    sequencer ! FSequencer.work(op, promise)
    promise.future
  }

  def withQueueSize(f: Int => Unit) = sequencer ! FSequencer.WithQueueSize(f)
}

object FutureSequencer {

  import scala.util.Try

  private final class FSequencer(
      receiveTimeout: Option[FiniteDuration],
      executionTimeout: Option[FiniteDuration] = None) extends Actor {

    receiveTimeout.foreach(context.setReceiveTimeout)

    private def idle: Receive = {

      case msg =>
        context become busy
        processThenDone(msg)
    }

    private def busy: Receive = {

      case Done => dequeue match {
        case None       => context become idle
        case Some(work) => processThenDone(work)
      }

      case msg => queue enqueue msg
    }

    def receive = idle

    private val queue = collection.mutable.Queue[Any]()
    private def dequeue: Option[Any] = Try(queue.dequeue).toOption

    private case object Done

    private def processThenDone(work: Any) {
      work match {
        case ReceiveTimeout => self ! PoisonPill
        case FSequencer.Work(run, promise, timeoutOption) =>
          promise completeWith timeoutOption.orElse(executionTimeout).fold(run()) { timeout =>
            run().withTimeout(
              duration = timeout,
              error = lila.common.LilaException(s"FSequencer timed out after $timeout")
            )(context.system)
          }.andThenAnyway {
            self ! Done
          }
        case FSequencer.WithQueueSize(f) => f(queue.size)
        case x => logwarn(s"[FSequencer] Unsupported message $x")
      }
    }
  }

  private object FSequencer {

    case class Work[A](
      run: () => Fu[A],
      promise: Promise[A],
      timeout: Option[FiniteDuration] = None)

    def work[A](
      run: => Fu[A],
      promise: Promise[A],
      timeout: Option[FiniteDuration] = None): Work[A] = Work(() => run, promise, timeout)

    case class WithQueueSize(f: Int => Unit)
  }
}
