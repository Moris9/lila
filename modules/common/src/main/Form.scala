package lila.common

import play.api.data.Forms._
import play.api.data.format.Formats._

object Form {

  def options(it: Iterable[Int], pattern: String) = it map { d =>
    d -> (pluralize(pattern, d) format d)
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
    nonEmptyText.verifying(hasKey(choices, _))

  def hasKey[A](choices: Iterable[(A, _)], key: A) =
    choices.map(_._1).toList contains key

  private def pluralize(pattern: String, nb: Int) =
    pattern.replace("{s}", (nb != 1).fold("s", ""))

  private def pluralize(pattern: String, nb: Double) =
    pattern.replace("{s}", (nb < 1).fold("s", ""))
}
