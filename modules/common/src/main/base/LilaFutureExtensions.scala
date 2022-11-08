package lila.base

import akka.actor.Scheduler
import alleycats.Zero
import scala.collection.BuildFrom
import scala.concurrent.duration.*
import scala.concurrent.{ Await, ExecutionContext as EC, Future }
import scala.util.Try

import lila.common.Chronometer

trait LilaFutureExtensions extends LilaTypes:

  extension [A](fua: Fu[A])

    inline def dmap[B](f: A => B): Fu[B]       = fua.map(f)(EC.parasitic)
    inline def dforeach[B](f: A => Unit): Unit = fua.foreach(f)(EC.parasitic)

    def >>-(sideEffect: => Unit)(implicit ec: EC): Fu[A] =
      fua andThen { case _ =>
        sideEffect
      }

    def >>[B](fub: => Fu[B])(implicit ec: EC): Fu[B] =
      fua flatMap { _ =>
        fub
      }

    inline def void: Fu[Unit] =
      dmap { _ =>
        ()
      }

    inline def inject[B](b: => B): Fu[B] =
      dmap { _ =>
        b
      }

    def injectAnyway[B](b: => B)(implicit ec: EC): Fu[B] = fold(_ => b, _ => b)

    def effectFold(fail: Exception => Unit, succ: A => Unit)(implicit ec: EC): Unit =
      fua onComplete {
        case scala.util.Failure(e: Exception) => fail(e)
        case scala.util.Failure(e)            => throw e // Throwables
        case scala.util.Success(e)            => succ(e)
      }

    def fold[B](fail: Exception => B, succ: A => B)(implicit ec: EC): Fu[B] =
      fua map succ recover { case e: Exception => fail(e) }

    def flatFold[B](fail: Exception => Fu[B], succ: A => Fu[B])(implicit ec: EC): Fu[B] =
      fua flatMap succ recoverWith { case e: Exception => fail(e) }

    def logFailure(logger: => lila.log.Logger, msg: Throwable => String)(implicit ec: EC): Fu[A] =
      addFailureEffect { e =>
        logger.warn(msg(e), e)
      }
    def logFailure(logger: => lila.log.Logger)(implicit ec: EC): Fu[A] = logFailure(logger, _.toString)

    def addFailureEffect(effect: Throwable => Unit)(implicit ec: EC) =
      fua.failed.foreach { (e: Throwable) =>
        effect(e)
      }
      fua

    def addEffect(effect: A => Unit)(implicit ec: EC): Fu[A] =
      fua foreach effect
      fua

    def addEffects(fail: Exception => Unit, succ: A => Unit)(implicit ec: EC): Fu[A] =
      fua onComplete {
        case scala.util.Failure(e: Exception) => fail(e)
        case scala.util.Failure(e)            => throw e // Throwables
        case scala.util.Success(e)            => succ(e)
      }
      fua

    def addEffects(f: Try[A] => Unit)(implicit ec: EC): Fu[A] =
      fua onComplete f
      fua

    def addEffectAnyway(inAnyCase: => Unit)(implicit ec: EC): Fu[A] =
      fua onComplete { _ =>
        inAnyCase
      }
      fua

    def mapFailure(f: Exception => Exception)(implicit ec: EC): Fu[A] =
      fua recoverWith { case cause: Exception =>
        fufail(f(cause))
      }

    def prefixFailure(p: => String)(implicit ec: EC): Fu[A] =
      mapFailure { e =>
        LilaException(s"$p ${e.getMessage}")
      }

    def thenPp(implicit ec: EC): Fu[A] =
      effectFold(
        e => println("[failure] " + e),
        a => println("[success] " + a)
      )
      fua

    def thenPp(msg: String)(implicit ec: EC): Fu[A] =
      effectFold(
        e => println(s"[$msg] [failure] $e"),
        a => println(s"[$msg] [success] $a")
      )
      fua

    def await(duration: FiniteDuration, name: String): A =
      Chronometer.syncMon(_.blocking.time(name)) {
        try
          Await.result(fua, duration)
        catch
          case e: Exception =>
            lila.mon.blocking.timeout(name).increment()
            throw e
      }

    def awaitOrElse(duration: FiniteDuration, name: String, default: => A): A =
      try
        await(duration, name)
      catch
        case _: Exception => default

    def withTimeout(duration: FiniteDuration)(implicit ec: EC, scheduler: Scheduler): Fu[A] =
      withTimeout(duration, LilaTimeout(s"Future timed out after $duration"))

    def withTimeout(
        duration: FiniteDuration,
        error: => Throwable
    )(implicit ec: EC, scheduler: Scheduler): Fu[A] =
      Future firstCompletedOf Seq(
        fua,
        akka.pattern.after(duration, scheduler)(Future failed error)
      )

    def withTimeoutDefault(
        duration: FiniteDuration,
        default: => A
    )(implicit ec: EC, scheduler: Scheduler): Fu[A] =
      Future firstCompletedOf Seq(
        fua,
        akka.pattern.after(duration, scheduler)(Future(default))
      )

    def delay(duration: FiniteDuration)(implicit ec: EC, scheduler: Scheduler) =
      lila.common.Future.delay(duration)(fua)

    def chronometer    = Chronometer(fua)
    def chronometerTry = Chronometer.lapTry(fua)

    def mon(path: lila.mon.TimerPath): Fu[A]              = chronometer.mon(path).result
    def monTry(path: Try[A] => lila.mon.TimerPath): Fu[A] = chronometerTry.mon(r => path(r)(lila.mon)).result
    def monSuccess(path: lila.mon.type => Boolean => kamon.metric.Timer): Fu[A] =
      chronometerTry.mon { r =>
        path(lila.mon)(r.isSuccess)
      }.result
    def monValue(path: A => lila.mon.TimerPath): Fu[A] = chronometer.monValue(path).result

    def logTime(name: String): Fu[A]                               = chronometer pp name
    def logTimeIfGt(name: String, duration: FiniteDuration): Fu[A] = chronometer.ppIfGt(name, duration)

    def recoverDefault(implicit z: Zero[A], ec: EC): Fu[A] = recoverDefault(z.zero)

    def recoverDefault(default: => A)(implicit ec: EC): Fu[A] =
      fua recover {
        case _: LilaException                         => default
        case _: java.util.concurrent.TimeoutException => default
        case e: Exception =>
          lila.log("common").warn("Future.recoverDefault", e)
          default
      }

  extension (fua: Fu[Boolean])

    def >>&(fub: => Fu[Boolean]): Fu[Boolean] =
      fua.flatMap { if (_) fub else fuFalse }(EC.parasitic)

    def >>|(fub: => Fu[Boolean]): Fu[Boolean] =
      fua.flatMap { if (_) fuTrue else fub }(EC.parasitic)

    inline def unary_! = fua.map { !_ }(EC.parasitic)

  extension [A](fua: Fu[Option[A]])

    def orFail(msg: => String)(implicit ec: EC): Fu[A] =
      fua flatMap {
        _.fold[Fu[A]](fufail(msg))(fuccess)
      }

    def orFailWith(err: => Exception)(implicit ec: EC): Fu[A] =
      fua flatMap {
        _.fold[Fu[A]](fufail(err))(fuccess)
      }

    def orElse(other: => Fu[Option[A]])(implicit ec: EC): Fu[Option[A]] =
      fua flatMap {
        _.fold(other) { x =>
          fuccess(Some(x))
        }
      }

    def getOrElse(other: => Fu[A])(implicit ec: EC): Fu[A] = fua flatMap { _.fold(other)(fuccess) }

    def map2[B](f: A => B)(implicit ec: EC): Fu[Option[B]] = fua.map(_ map f)
    def dmap2[B](f: A => B): Fu[Option[B]]                 = fua.map(_ map f)(EC.parasitic)

    def getIfPresent: Option[A] =
      fua.value match
        case Some(scala.util.Success(v)) => v
        case _                           => None

  extension [A, M[X] <: IterableOnce[X]](t: M[Fu[A]])
    def sequenceFu(implicit bf: BuildFrom[M[Fu[A]], A, M[A]], ec: EC): Fu[M[A]] = Future.sequence(t)
