package lila

import ornicar.scalalib

import scalaz.{ Zero, Functor, Monad, OptionT }
import scala.concurrent.Future

trait PackageObject
    extends WithFuture
    with scalalib.Validation
    with scalalib.Common
    with scalalib.Regex
    with scalalib.IO
    with scalalib.DateTime
    with scalaz.Identitys
    with scalaz.NonEmptyLists
    with scalaz.Strings
    with scalaz.Lists
    with scalaz.Zeros
    with scalaz.Booleans
    with scalaz.Options
    with scalaz.OptionTs {

  val toVoid = (_: Any) ⇒ ()

  def !![A](msg: String): Valid[A] = msg.failNel[A]

  def nowMillis: Double = System.currentTimeMillis
  def nowSeconds: Int = (nowMillis / 1000).toInt

  implicit final class LilaPimpedOption[A](o: Option[A]) {

    def zmap[B](f: A ⇒ B)(implicit z: Zero[B]) = o.fold(z.zero)(f)
  }

  implicit final class LilaPimpedMap[A, B](m: Map[A, B]) {

    def +?(bp: (Boolean, (A, B))): Map[A, B] = if (bp._1) m + bp._2 else m
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

  def printToFile(f: java.io.File)(op: java.io.PrintWriter ⇒ Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def printToFile(f: String)(op: java.io.PrintWriter ⇒ Unit) {
    printToFile(new java.io.File(f))(op)
  }
}

trait WithFuture extends scalaz.Zeros {

  import spray.util.pimps.PimpedFuture

  type Fu[A] = Future[A]
  type Funit = Fu[Unit]

  def fuccess[A](a: A) = Future successful a
  def fufail[A <: Throwable, B](a: A): Fu[B] = Future failed a
  def fufail[B](a: String): Fu[B] = Future failed (new RuntimeException(a))
  def funit = fuccess(())

  implicit def LilaFuZero[A: Zero]: Zero[Fu[A]] = new Zero[Fu[A]] { val zero = fuccess(∅[A]) }

  implicit def SprayPimpedFuture[T](fut: Future[T]): PimpedFuture[T] = new PimpedFuture[T](fut)
}

trait WithDb { self: PackageObject ⇒

  // implicit def reactiveSortJsObject(sort: (String, SortOrder)): (String, JsValueWrapper) = sort match {
  //   case (field, SortOrder.Ascending) ⇒ field -> 1
  //   case (field, _)                   ⇒ field -> -1
  // }
}

trait WithPlay { self: PackageObject ⇒

  import play.api.libs.json._
  import play.api.libs.concurrent.Promise
  import play.api.libs.iteratee.{ Iteratee, Enumerator }
  import play.api.libs.iteratee.Concurrent.Channel
  import play.api.Play.current
  import play.api.libs.concurrent.Execution.Implicits._

  type JsChannel = Channel[JsValue]
  type JsEnumerator = Enumerator[JsValue]
  type SocketFuture = Fu[(Iteratee[JsValue, _], JsEnumerator)]

  // Typeclasses
  implicit def LilaFutureFunctor = new Functor[Fu] {
    def fmap[A, B](r: Fu[A], f: A ⇒ B) = r map f
  }
  implicit def LilaFutureMonad = new Monad[Fu] {
    def pure[A](a: ⇒ A) = fuccess(a)
    def bind[A, B](r: Fu[A], f: A ⇒ Fu[B]) = r flatMap f
  }

  implicit final class LilaPimpedFuture[A](fua: Fu[A]) {

    def >>(sideEffect: ⇒ Unit): Funit = >>(fuccess(sideEffect))

    def >>[B](fub: Fu[B]): Fu[B] = fua flatMap (_ ⇒ fub)

    def void: Funit = fua map (_ ⇒ Unit)

    def inject[B](b: B): Fu[B] = fua map (_ ⇒ b)
  }

  implicit final class LilaPimpedFutureZero[A: Zero](fua: Fu[A]) {

    def doIf(cond: Boolean): Fu[A] = cond.fold(fua, fuccess(∅[A]))

    def doUnless(cond: Boolean): Fu[A] = doIf(!cond)
  }

  implicit final class LilaPimpedJsObject(obj: JsObject) {

    def get[T: Reads](field: String): Option[T] = (obj \ field) match {
      case JsUndefined(_) ⇒ none
      case value          ⇒ value.asOpt[T]
    }
  }
}
