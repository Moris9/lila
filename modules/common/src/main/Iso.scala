package lila.common

import chess.Centis
import chess.format.{ FEN, Uci }
import play.api.i18n.Lang

trait Iso[A, B]:
  val from: A => B
  val to: B => A

  def map[BB](mapFrom: B => BB, mapTo: BB => B) = new Iso[A, BB]:
    val from = a => mapFrom(Iso.this.from(a))
    val to   = bb => Iso.this.to(mapTo(bb))

object Iso:

  type StringIso[B]     = Iso[String, B]
  type IntIso[B]        = Iso[Int, B]
  type BooleanIso[B]    = Iso[Boolean, B]
  type DoubleIso[B]     = Iso[Double, B]
  type FloatIso[B]      = Iso[Float, B]
  type BigDecimalIso[B] = Iso[BigDecimal, B]

  def apply[A, B](f: A => B, t: B => A): Iso[A, B] =
    new Iso[A, B]:
      val from = f
      val to   = t

  def string[B](from: String => B, to: B => String): StringIso[B] = apply(from, to)
  def int[B](from: Int => B, to: B => Int): IntIso[B]             = apply(from, to)
  def double[B](from: Double => B, to: B => Double): DoubleIso[B] = apply(from, to)
  def float[B](from: Float => B, to: B => Float): FloatIso[B]     = apply(from, to)

  def strings(sep: String): StringIso[Strings] =
    Iso[String, Strings](
      str => Strings(str.split(sep).iterator.map(_.trim).toList),
      strs => strs.value mkString sep
    )
  def userIds(sep: String): StringIso[UserIds] =
    Iso[String, UserIds](
      str => UserIds(str.split(sep).iterator.map(_.trim.toLowerCase).toList),
      strs => strs.value mkString sep
    )
  def ints(sep: String): StringIso[Ints] =
    Iso[String, Ints](
      str => Ints(str.split(sep).iterator.map(_.trim).flatMap(_.toIntOption).toList),
      strs => strs.value mkString sep
    )

  def opaque[A <: String](from: String => A): StringIso[A] = apply(from, identity)

  given isoIdentity[A]: Iso[A, A] = apply(identity[A], identity[A])

  given StringIso[IpAddress] = string[IpAddress](IpAddress.unchecked, _.value)

  given StringIso[EmailAddress] = string[EmailAddress](EmailAddress.apply, _.value)

  given StringIso[NormalizedEmailAddress] =
    string[NormalizedEmailAddress](NormalizedEmailAddress.apply, _.value)

  given IntIso[Centis] = int[Centis](Centis.apply, _.centis)

  given StringIso[Lang] = string[Lang](Lang.apply, _.toString)

  given StringIso[FEN] = string[FEN](FEN.apply, _.value)

  given StringIso[Markdown] = string[Markdown](Markdown.apply, _.value)
