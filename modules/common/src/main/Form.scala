package lila.common

import org.joda.time.DateTimeZone
import play.api.data.format.Formats._
import play.api.data.format.Formatter
import play.api.data.Forms._

object Form {

  def options(it: Iterable[Int], pattern: String) = it map { d =>
    d -> (pluralize(pattern, d) format d)
  }

  def options(it: Iterable[Int], transformer: Int => Int, pattern: String) = it map { d =>
    d -> (pluralize(pattern, transformer(d)) format transformer(d))
  }

  def options(it: Iterable[Int], code: String, pattern: String) = it map { d =>
    (d + code) -> (pluralize(pattern, d) format d)
  }

  def optionsDouble(it: Iterable[Double], format: Double => String) = it map { d =>
    d -> format(d)
  }

  def numberIn(choices: Iterable[(Int, String)]) =
    number.verifying(hasKey(choices, _))

  def numberInDouble(choices: Iterable[(Double, String)]) =
    of[Double].verifying(hasKey(choices, _))

  def stringIn(choices: Iterable[(String, String)]) =
    text.verifying(hasKey(choices, _))

  def hasKey[A](choices: Iterable[(A, _)], key: A) =
    choices.map(_._1).toList contains key

  private def pluralize(pattern: String, nb: Int) =
    pattern.replace("{s}", (nb != 1).fold("s", ""))

  object formatter {
    def stringFormatter[A](from: A => String, to: String => A): Formatter[A] = new Formatter[A] {
      def bind(key: String, data: Map[String, String]) = stringFormat.bind(key, data).right map to
      def unbind(key: String, value: A) = stringFormat.unbind(key, from(value))
    }
    def intFormatter[A](from: A => Int, to: Int => A): Formatter[A] = new Formatter[A] {
      def bind(key: String, data: Map[String, String]) = intFormat.bind(key, data).right map to
      def unbind(key: String, value: A) = intFormat.unbind(key, from(value))
    }
  }

  object UTCDate {
    val dateTimePattern = "yyyy-MM-dd HH:mm"
    val utcDate = jodaDate(dateTimePattern, DateTimeZone.UTC)
    implicit val dateTimeFormat = jodaDateTimeFormat(dateTimePattern)
  }
}
