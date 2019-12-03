package lila.db

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.bson.exceptions.TypeDoesNotMatchException
import scala.util.{ Try, Success, Failure }
import scalaz.NonEmptyList

import lila.common.Iso._
import lila.common.{ Iso, IpAddress, EmailAddress, NormalizedEmailAddress }

trait Handlers {

  implicit val BSONJodaDateTimeHandler = dsl.quickHandler[DateTime](
    { case v: BSONDateTime => new DateTime(v.value) },
    v => BSONDateTime(v.getMillis)
  )

  def isoHandler[A, B](iso: Iso[B, A])(implicit handler: BSONHandler[B]): BSONHandler[A] = new BSONHandler[A] {
    def readTry(x: BSONValue) = handler.readTry(x) map iso.from
    def writeTry(x: A) = handler writeTry iso.to(x)
  }
  def isoHandler[A, B](to: A => B, from: B => A)(implicit handler: BSONHandler[B]): BSONHandler[A] =
    isoHandler(Iso(from, to))

  def stringIsoHandler[A](implicit iso: StringIso[A]): BSONHandler[A] = isoHandler[A, String](iso)
  def stringAnyValHandler[A](to: A => String, from: String => A): BSONHandler[A] = stringIsoHandler(Iso(from, to))

  def intIsoHandler[A](implicit iso: IntIso[A]): BSONHandler[A] = isoHandler[A, Int](iso)
  def intAnyValHandler[A](to: A => Int, from: Int => A): BSONHandler[A] = intIsoHandler(Iso(from, to))

  def booleanIsoHandler[A](implicit iso: BooleanIso[A]): BSONHandler[A] = isoHandler[A, Boolean](iso)
  def booleanAnyValHandler[A](to: A => Boolean, from: Boolean => A): BSONHandler[A] = booleanIsoHandler(Iso(from, to))

  def doubleIsoHandler[A](implicit iso: DoubleIso[A]): BSONHandler[A] = isoHandler[A, Double](iso)
  def doubleAnyValHandler[A](to: A => Double, from: Double => A): BSONHandler[A] = doubleIsoHandler(Iso(from, to))

  def dateIsoHandler[A](implicit iso: Iso[DateTime, A]): BSONHandler[A] = isoHandler[A, DateTime](iso)

  def quickHandler[T](read: PartialFunction[BSONValue, T], write: T => BSONValue): BSONHandler[T] = new BSONHandler[T] {
    def readTry(bson: BSONValue) = read.andThen(Success(_)).applyOrElse(
      bson,
      (b: BSONValue) => handlerBadType(b)
    )
    def writeTry(t: T) = Success(write(t))
  }

  def tryHandler[T](read: PartialFunction[BSONValue, Try[T]], write: T => BSONValue): BSONHandler[T] = new BSONHandler[T] {
    def readTry(bson: BSONValue) = read.applyOrElse(
      bson,
      (b: BSONValue) => handlerBadType(b)
    )
    def writeTry(t: T) = Success(write(t))
  }

  def handlerBadType[T](b: BSONValue): Try[T] =
    Failure(TypeDoesNotMatchException("BSONBinary", b.getClass.getSimpleName))

  def handlerBadValue[T](msg: String): Try[T] =
    Failure(new IllegalArgumentException(msg))

  implicit def bsonArrayToNonEmptyListHandler[T](implicit handler: BSONHandler[T]) = {
    def listWriter = collectionWriter[T, List[T]]
    def listReader = collectionReader[List, T]
    tryHandler[NonEmptyList[T]](
      {
        case array: BSONArray =>
          listReader.readTry(array).flatMap {
            _.toNel toTry s"BSONArray is empty, can't build NonEmptyList"
          }
      },
      nel => listWriter.writeTry(nel.toList).get
    )
  }

  implicit val ipAddressHandler = isoHandler[IpAddress, String](ipAddressIso)

  implicit val emailAddressHandler = isoHandler[EmailAddress, String](emailAddressIso)

  implicit val normalizedEmailAddressHandler = isoHandler[NormalizedEmailAddress, String](normalizedEmailAddressIso)

  implicit val colorBoolHandler = BSONBooleanHandler.as[chess.Color](chess.Color.apply, _.white)
}
