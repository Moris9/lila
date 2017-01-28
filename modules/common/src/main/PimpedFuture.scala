package lila

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ Future, ExecutionContext }

object PimpedFuture {

  private type Fu[A] = Future[A]

  object DirectExecutionContext extends ExecutionContext {
    override def execute(command: Runnable): Unit = command.run()
    override def reportFailure(cause: Throwable): Unit =
      throw new IllegalStateException("lila DirectExecutionContext failure", cause)
  }

  final class LilaPimpedFuture[A](val fua: Fu[A]) extends AnyVal {

    def dmap[B](f: A => B): Fu[B] = fua.map(f)(DirectExecutionContext)
    def dforeach[B](f: A => Unit): Unit = fua.foreach(f)(DirectExecutionContext)

    def >>-(sideEffect: => Unit): Fu[A] = fua andThen {
      case _ => sideEffect
    }

    def >>[B](fub: => Fu[B]): Fu[B] = fua flatMap (_ => fub)

    def void: Fu[Unit] = fua.map(_ => ())(DirectExecutionContext)

    def inject[B](b: => B): Fu[B] = fua.map(_ => b)(DirectExecutionContext)

    def injectAnyway[B](b: => B): Fu[B] = fold(_ => b, _ => b)

    def effectFold(fail: Exception => Unit, succ: A => Unit) {
      fua onComplete {
        case scala.util.Failure(e: Exception) => fail(e)
        case scala.util.Failure(e)            => throw e // Throwables
        case scala.util.Success(e)            => succ(e)
      }
    }

    def fold[B](fail: Exception => B, succ: A => B): Fu[B] =
      fua map succ recover { case e: Exception => fail(e) }

    def flatFold[B](fail: Exception => Fu[B], succ: A => Fu[B]): Fu[B] =
      fua flatMap succ recoverWith { case e: Exception => fail(e) }

    def logFailure(logger: => lila.log.Logger, msg: Exception => String): Fu[A] =
      addFailureEffect { e => logger.warn(msg(e), e) }
    def logFailure(logger: => lila.log.Logger): Fu[A] = logFailure(logger, _.toString)

    def addFailureEffect(effect: Exception => Unit) = {
      fua onFailure {
        case e: Exception => effect(e)
      }
      fua
    }

    def addEffect(effect: A => Unit): Fu[A] = {
      fua foreach effect
      fua
    }

    def addEffects(fail: Exception => Unit, succ: A => Unit): Fu[A] = {
      fua onComplete {
        case scala.util.Failure(e: Exception) => fail(e)
        case scala.util.Failure(e)            => throw e // Throwables
        case scala.util.Success(e)            => succ(e)
      }
      fua
    }

    def addEffectAnyway(inAnyCase: => Unit): Fu[A] = {
      fua onComplete {
        case _ => inAnyCase
      }
      fua
    }

    def mapFailure(f: Exception => Exception) = fua recover {
      case cause: Exception => throw f(cause)
    }

    def prefixFailure(p: => String) = mapFailure { e =>
      common.LilaException(s"$p ${e.getMessage}")
    }

    def thenPp: Fu[A] = {
      effectFold(
        e => println("[failure] " + e),
        a => println("[success] " + a)
      )
      fua
    }

    def thenPp(msg: String): Fu[A] = {
      effectFold(
        e => println(s"[$msg] [failure] $e"),
        a => println(s"[$msg] [success] $a")
      )
      fua
    }

    def await(duration: FiniteDuration): A =
      scala.concurrent.Await.result(fua, duration)

    def awaitSeconds(seconds: Int): A =
      await(seconds.seconds)

    def withTimeout(duration: FiniteDuration, error: => Throwable)(implicit system: akka.actor.ActorSystem): Fu[A] = {
      Future firstCompletedOf Seq(
        fua,
        akka.pattern.after(duration, system.scheduler)(Future failed error))
    }

    def withTimeoutDefault(duration: FiniteDuration, default: => A)(implicit system: akka.actor.ActorSystem): Fu[A] = {
      Future firstCompletedOf Seq(
        fua,
        akka.pattern.after(duration, system.scheduler)(Future(default)))
    }

    def chronometer = lila.common.Chronometer(fua)

    def mon(path: lila.mon.RecPath) = chronometer.mon(path).result
  }
}
