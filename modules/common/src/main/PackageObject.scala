package lila

import scala.concurrent.Future

import ornicar.scalalib
import scalaz._
import Scalaz._

trait PackageObject extends WithFuture

    with scalalib.Validation
    with scalalib.Common
    with scalalib.Regex
    with scalalib.DateTime

    with scalaz.std.BooleanFunctions
    with scalaz.syntax.std.ToBooleanOps

    with scalaz.std.OptionInstances
    with scalaz.std.OptionFunctions
    with scalaz.syntax.std.ToOptionOps
    with scalaz.syntax.std.ToOptionIdOps

    with scalaz.std.ListInstances
    with scalaz.std.ListFunctions
    with scalaz.syntax.std.ToListOps

    with scalaz.syntax.ToShowOps {
  // with scalaz.Identitys
  // with scalaz.NonEmptyLists
  // with scalaz.Strings
  // with scalaz.Lists
  // with scalaz.OptionTs 
  // with scalaz.Booleans
  // with scalaz.Options

  def !![A](msg: String): Valid[A] = msg.failNel[A]

  def nowMillis: Long = System.currentTimeMillis
  def nowSeconds: Int = (nowMillis / 1000).toInt

  lazy val logger = play.api.Logger("lila")
  def loginfo(s: String) { logger info s }
  def logwarn(s: String) { logger warn s }
  def logerr(s: String) { logger error s }
  def fuloginfo(s: String) = fuccess { loginfo(s) }
  def fulogwarn(s: String) = fuccess { logwarn(s) }
  def fulogerr(s: String) = fuccess { logerr(s) }

  implicit final class LilaPimpedOption[A](o: Option[A]) {

    def ??[B](f: A ⇒ B)(implicit m: Monoid[B]): B = o.fold(m.zero)(f)

    def ifTrue(b: Boolean): Option[A] = o filter (_ ⇒ b)
    def ifFalse(b: Boolean): Option[A] = o filter (_ ⇒ !b)
  }

  implicit final class LilaPimpedString(s: String) {

    def describes[A](v: ⇒ A): A = { loginfo(s); v }
  }

  implicit final class LilaPimpedValid[A](v: Valid[A]) {

    def future: Fu[A] = v fold (errs ⇒ fufail(errs.shows), fuccess)
  }

  def parseIntOption(str: String): Option[Int] = try {
    Some(java.lang.Integer.parseInt(str))
  }
  catch {
    case e: NumberFormatException ⇒ None
  }

  def parseFloatOption(str: String): Option[Float] = try {
    Some(java.lang.Float.parseFloat(str))
  }
  catch {
    case e: NumberFormatException ⇒ None
  }

  def intBox(in: Range.Inclusive)(v: Int): Int =
    math.max(in.start, math.min(v, in.end))

  def floatBox(in: Range.Inclusive)(v: Float): Float =
    math.max(in.start, math.min(v, in.end))
}

trait WithFuture extends scalalib.Validation {

  type Fu[A] = Future[A]
  type Funit = Fu[Unit]

  def fuccess[A](a: A) = Future successful a
  def fufail[A <: Exception, B](a: A): Fu[B] = Future failed a
  def fufail[A](a: String): Fu[A] = fufail(common.LilaException(a))
  def fufail[A](a: Failures): Fu[A] = fufail(common.LilaException(a))
  val funit = fuccess(())

  implicit def SprayPimpedFuture[T](fut: Future[T]) =
    new spray.util.pimps.PimpedFuture[T](fut)
}

trait WithPlay { self: PackageObject ⇒

  import play.api.libs.json._

  implicit def execontext = play.api.libs.concurrent.Execution.defaultContext

  implicit val LilaFutureInstances = new Monad[Fu] {
    override def map[A, B](fa: Fu[A])(f: A ⇒ B) = fa map f
    def point[A](a: ⇒ A) = fuccess(a)
    def bind[A, B](fa: Fu[A])(f: A ⇒ Fu[B]) = fa flatMap f
  }

  implicit def LilaFuMonoid[A](implicit m: Monoid[A]): Monoid[Fu[A]] =
    Monoid.instance((x, y) ⇒ x zip y map {
      case (a, b) ⇒ m.append(a, b)
    }, fuccess(m.zero))

  implicit val LilaJsObjectMonoid: Monoid[JsObject] =
    Monoid.instance((x, y) ⇒ x ++ y, JsObject(Seq.empty))

  // implicit def LilaJsResultMonoid[A]: Monoid[JsResult[A]] =
  //   Monoid.instance((x, y) ⇒ x ++ y, JsError(Seq.empty))

  implicit final class LilaTraversableFuture[A, M[_] <: TraversableOnce[_]](t: M[Fu[A]]) {

    def sequenceFu(implicit cbf: scala.collection.generic.CanBuildFrom[M[Fu[A]], A, M[A]]) = Future sequence t
  }

  implicit final class LilaPimpedFuture[A](fua: Fu[A]) {

    def >>-(sideEffect: ⇒ Unit): Funit = fua.void andThen {
      case _ ⇒ sideEffect
    }

    def >>[B](fub: ⇒ Fu[B]): Fu[B] = fua flatMap (_ ⇒ fub)

    def void: Funit = fua map (_ ⇒ Unit)

    def inject[B](b: ⇒ B): Fu[B] = fua map (_ ⇒ b)

    def effectFold(fail: Exception ⇒ Unit, succ: A ⇒ Unit) {
      fua onComplete {
        case scala.util.Failure(e: Exception) ⇒ fail(e)
        case scala.util.Failure(e)            ⇒ throw e // Throwables
        case scala.util.Success(e)            ⇒ succ(e)
      }
    }

    def fold[B](fail: Exception ⇒ B, succ: A ⇒ B): Fu[B] =
      fua map succ recover { case e: Exception ⇒ fail(e) }

    def flatFold[B](fail: Exception ⇒ Fu[B], succ: A ⇒ Fu[B]): Fu[B] =
      fua flatMap succ recoverWith { case e: Exception ⇒ fail(e) }

    def logFailure(prefix: Throwable ⇒ String): Fu[A] = fua ~ (_ onFailure {
      case e: Exception ⇒ logwarn(prefix(e) + " " + e.getMessage)
    })
    def logFailure(prefix: ⇒ String): Fu[A] = fua ~ (_ onFailure {
      case e: Exception ⇒ logwarn(prefix + " " + e.getMessage)
    })

    def addEffect(effect: A ⇒ Unit) = fua ~ (_ foreach effect)

    def addFailureEffect(effect: Exception ⇒ Unit) = fua ~ (_ onFailure {
      case e: Exception ⇒ effect(e)
    })

    def thenPp: Fu[A] = fua ~ {
      _.effectFold(
        e ⇒ logwarn("[failure] " + e),
        a ⇒ loginfo("[success] " + a)
      )
    }
  }

  implicit final class LilaPimpedFutureMonoid[A](fua: Fu[A])(implicit m: Monoid[A]) {

    def nevermind(msg: String): Fu[A] = fua recover {
      case e: lila.common.LilaException             ⇒ recoverException(e, msg.some)
      case e: java.util.concurrent.TimeoutException ⇒ recoverException(e, msg.some)
    }

    def nevermind: Fu[A] = nevermind("")

    private def recoverException(e: Exception, msg: Option[String]) = {
      logwarn(msg.filter(_.nonEmpty).??(_ + ": ") + e.getMessage)
      m.zero
    }
  }

  implicit final class LilaPimpedFutureOption[A](fua: Fu[Option[A]]) {

    def flatten(msg: ⇒ String): Fu[A] = fua flatMap {
      _.fold[Fu[A]](fufail(msg))(fuccess(_))
    }

    def orElse(other: ⇒ Fu[Option[A]]): Fu[Option[A]] = fua flatMap {
      _.fold(other) { x ⇒ fuccess(x.some) }
    }
  }

  implicit final class LilaPimpedFutureBoolean(fua: Fu[Boolean]) {

    def >>&(fub: ⇒ Fu[Boolean]): Fu[Boolean] =
      fua flatMap { _.fold(fub, fuccess(false)) }

    def >>|(fub: ⇒ Fu[Boolean]): Fu[Boolean] =
      fua flatMap { _.fold(fuccess(true), fub) }

    def unary_! = fua map (!_)
  }

  implicit final class LilaPimpedBooleanForFuture(b: Boolean) {

    def optionFu[A](v: ⇒ Fu[A]): Fu[Option[A]] =
      if (b) v map (_.some) else fuccess(none)
  }

  object makeTimeout {

    import akka.util.Timeout
    import scala.concurrent.duration._

    implicit val short = seconds(1)
    implicit val large = seconds(5)
    implicit val veryLarge = minutes(5)

    def apply(duration: FiniteDuration) = Timeout(duration)
    def seconds(s: Int): Timeout = Timeout(s.seconds)
    def minutes(m: Int): Timeout = Timeout(m.minutes)
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter ⇒ Unit): Funit = Future {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def printToFile(f: String)(op: java.io.PrintWriter ⇒ Unit): Funit =
    printToFile(new java.io.File(f))(op)
}
