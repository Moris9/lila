package lila.db

import cats.data.NonEmptyList
import chess.format.FEN
import chess.opening.OpeningFamily
import chess.variant.Variant
import org.joda.time.DateTime
import reactivemongo.api.bson.*
import reactivemongo.api.bson.exceptions.TypeDoesNotMatchException
import scala.concurrent.duration.*
import scala.util.{ Failure, Success, Try }

import lila.common.Iso.*
import lila.common.{ EmailAddress, IpAddress, Iso, NormalizedEmailAddress }
import scala.collection.Factory

trait Handlers:

  given dateTimeHandler: BSONHandler[DateTime] = quickHandler[DateTime](
    { case v: BSONDateTime => new DateTime(v.value) },
    v => BSONDateTime(v.getMillis)
  )

  def isoHandler[A, B](using iso: Iso[B, A])(using handler: BSONHandler[B]): BSONHandler[A] =
    new BSONHandler[A]:
      def readTry(x: BSONValue) = handler.readTry(x) map iso.from
      def writeTry(x: A)        = handler writeTry iso.to(x)
  def isoHandler[A, B](to: A => B, from: B => A)(using handler: BSONHandler[B]): BSONHandler[A] =
    isoHandler(using Iso(from, to))(using handler)

  def stringIsoHandler[A](implicit iso: StringIso[A]): BSONHandler[A] =
    BSONStringHandler.as[A](iso.from, iso.to)
  def stringAnyValHandler[A](to: A => String, from: String => A): BSONHandler[A] =
    stringIsoHandler(Iso(from, to))

  def intIsoHandler[A](implicit iso: IntIso[A]): BSONHandler[A] = BSONIntegerHandler.as[A](iso.from, iso.to)
  def intAnyValHandler[A](to: A => Int, from: Int => A): BSONHandler[A] = intIsoHandler(Iso(from, to))

  def booleanIsoHandler[A](implicit iso: BooleanIso[A]): BSONHandler[A] =
    BSONBooleanHandler.as[A](iso.from, iso.to)
  def booleanAnyValHandler[A](to: A => Boolean, from: Boolean => A): BSONHandler[A] =
    booleanIsoHandler(Iso(from, to))

  def doubleIsoHandler[A](implicit iso: DoubleIso[A]): BSONHandler[A] =
    BSONDoubleHandler.as[A](iso.from, iso.to)
  def doubleAnyValHandler[A](to: A => Double, from: Double => A): BSONHandler[A] =
    doubleIsoHandler(Iso(from, to))
  def doubleAsIntHandler[A](to: A => Double, from: Double => A, multiplier: Int): BSONHandler[A] =
    intAnyValHandler[A](x => Math.round(to(x) * multiplier).toInt, x => from(x.toDouble / multiplier))

  val percentBsonMultiplier = 1000
  val ratioBsonMultiplier   = 100_000

  def percentAsIntHandler[A](to: A => Double, from: Double => A): BSONHandler[A] =
    doubleAsIntHandler(to, from, percentBsonMultiplier)
  def ratioAsIntHandler[A](to: A => Double, from: Double => A): BSONHandler[A] =
    doubleAsIntHandler(to, from, ratioBsonMultiplier)

  def floatIsoHandler[A](implicit iso: FloatIso[A]): BSONHandler[A] =
    BSONFloatHandler.as[A](iso.from, iso.to)
  def floatAnyValHandler[A](to: A => Float, from: Float => A): BSONHandler[A] =
    floatIsoHandler(Iso(from, to))

  def bigDecimalIsoHandler[A](implicit iso: BigDecimalIso[A]): BSONHandler[A] =
    BSONDecimalHandler.as[A](iso.from, iso.to)

  def bigDecimalAnyValHandler[A](to: A => BigDecimal, from: BigDecimal => A): BSONHandler[A] =
    bigDecimalIsoHandler(Iso(from, to))

  def dateIsoHandler[A](implicit iso: Iso[DateTime, A]): BSONHandler[A] =
    dateTimeHandler.as[A](iso.from, iso.to)

  def quickHandler[T](read: PartialFunction[BSONValue, T], write: T => BSONValue): BSONHandler[T] =
    new BSONHandler[T]:
      def readTry(bson: BSONValue) =
        read
          .andThen(Success(_))
          .applyOrElse(bson, (b: BSONValue) => handlerBadType(b))
      def writeTry(t: T) = Success(write(t))

  def tryHandler[T](read: PartialFunction[BSONValue, Try[T]], write: T => BSONValue): BSONHandler[T] =
    new BSONHandler[T]:
      def readTry(bson: BSONValue) =
        read.applyOrElse(
          bson,
          (b: BSONValue) => handlerBadType(b)
        )
      def writeTry(t: T) = Success(write(t))

  def handlerBadType[T](b: BSONValue): Try[T] =
    Failure(TypeDoesNotMatchException("BSONValue", b.getClass.getSimpleName))

  def handlerBadValue[T](msg: String): Try[T] =
    Failure(new IllegalArgumentException(msg))

  def eitherHandler[L, R](implicit leftHandler: BSONHandler[L], rightHandler: BSONHandler[R]) =
    new BSONHandler[Either[L, R]]:
      def readTry(bson: BSONValue) =
        leftHandler.readTry(bson).map(Left.apply) orElse rightHandler.readTry(bson).map(Right.apply)
      def writeTry(e: Either[L, R]) = e.fold(leftHandler.writeTry, rightHandler.writeTry)

  def stringMapHandler[V](implicit
      reader: BSONReader[Map[String, V]],
      writer: BSONWriter[Map[String, V]]
  ) =
    new BSONHandler[Map[String, V]]:
      def readTry(bson: BSONValue)    = reader readTry bson
      def writeTry(v: Map[String, V]) = writer writeTry v

  def typedMapHandler[K, V: BSONReader: BSONWriter](keyIso: StringIso[K]) =
    stringMapHandler[V].as[Map[K, V]](
      _.map { case (k, v) => keyIso.from(k) -> v },
      _.map { case (k, v) => keyIso.to(k) -> v }
    )

  given [T](using handler: BSONHandler[T]): BSONHandler[NonEmptyList[T]] =
    def listWriter = BSONWriter.collectionWriter[T, List[T]]
    def listReader = collectionReader[List, T]
    tryHandler[NonEmptyList[T]](
      { case array: BSONArray =>
        listReader.readTry(array).flatMap {
          _.toNel toTry s"BSONArray is empty, can't build NonEmptyList"
        }
      },
      nel => listWriter.writeTry(nel.toList).get
    )

  given listHandler[T: BSONHandler]: BSONHandler[List[T]] with
    val reader                                 = collectionReader[List, T]
    val writer                                 = BSONWriter.collectionWriter[T, List[T]]
    def readTry(bson: BSONValue): Try[List[T]] = reader.readTry(bson)
    def writeTry(t: List[T]): Try[BSONValue]   = writer.writeTry(t)

  // given collectionHandler[T, M[_], Repr <: Iterable[T]](using
  //     handler: BSONHandler[T],
  //     factory: Factory[T, M[T]]
  // ): BSONHandler[M[T]] with
  //   val reader                              = collectionReader[M, T]
  //   val writer                              = BSONWriter.collectionWriter[T, Repr]
  //   def readTry(bson: BSONValue): Try[M[T]] = reader.readTry(bson)
  //   def writeTry(t: Repr): Try[BSONValue]   = writer.writeTry(t)

  given BSONWriter[BSONNull.type] with
    def writeTry(n: BSONNull.type) = Success(BSONNull)

  given BSONHandler[IpAddress] = stringIsoHandler

  given BSONHandler[EmailAddress] = stringIsoHandler

  given BSONHandler[NormalizedEmailAddress] = stringIsoHandler

  given BSONHandler[chess.Color]  = BSONBooleanHandler.as[chess.Color](chess.Color.fromWhite, _.white)
  given BSONHandler[chess.Centis] = intIsoHandler

  given BSONHandler[FEN] = stringIsoHandler

  import lila.common.{ LilaOpeningFamily, SimpleOpening }
  given BSONHandler[SimpleOpening.Key] = stringIsoHandler
  given BSONHandler[SimpleOpening] = tryHandler[SimpleOpening](
    { case BSONString(key) => SimpleOpening find key toTry s"No such opening: $key" },
    o => BSONString(o.key.value)
  )
  given BSONHandler[LilaOpeningFamily] = tryHandler[LilaOpeningFamily](
    { case BSONString(key) => LilaOpeningFamily find key toTry s"No such opening family: $key" },
    o => BSONString(o.key.value)
  )

  given BSONHandler[chess.Mode] = BSONBooleanHandler.as[chess.Mode](chess.Mode.apply, _.rated)

  given BSONHandler[lila.common.Days]     = isoHandler[lila.common.Days, Int]
  given BSONHandler[lila.common.Markdown] = isoHandler[lila.common.Markdown, String]

  given [T: BSONHandler]: BSONHandler[(T, T)] = tryHandler[(T, T)](
    { case arr: BSONArray => for { a <- arr.getAsTry[T](0); b <- arr.getAsTry[T](1) } yield (a, b) },
    { case (a, b) => BSONArray(a, b) }
  )

  val minutesHandler = BSONIntegerHandler.as[FiniteDuration](_.minutes, _.toMinutes.toInt)

  val variantByKeyHandler: BSONHandler[Variant] = quickHandler[Variant](
    {
      case BSONString(v) => Variant orDefault v
      case _             => Variant.default
    },
    v => BSONString(v.key)
  )

  val clockConfigHandler = tryHandler[chess.Clock.Config](
    { case doc: BSONDocument =>
      for {
        limit <- doc.getAsTry[Int]("limit")
        inc   <- doc.getAsTry[Int]("increment")
      } yield chess.Clock.Config(limit, inc)
    },
    c =>
      BSONDocument(
        "limit"     -> c.limitSeconds,
        "increment" -> c.incrementSeconds
      )
  )

  def valueMapHandler[K, V](mapping: Map[K, V])(toKey: V => K)(implicit
      keyHandler: BSONHandler[K]
  ) = new BSONHandler[V]:
    def readTry(bson: BSONValue) = keyHandler.readTry(bson) flatMap { k =>
      mapping.get(k) toTry s"No such value in mapping: $k"
    }
    def writeTry(v: V) = keyHandler writeTry toKey(v)
