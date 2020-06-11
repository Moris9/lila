package lila.common

import scala.Numeric.Implicits._
import scala.reflect.ClassTag
import scala.util.Sorting

object Maths {
  def mean[T](a: Iterable[T])(implicit n: Numeric[T]): Option[Double] =
    a.nonEmpty option (n.toDouble(a.sum) / a.size)

  def median[T: ClassTag](a: Iterable[T])(implicit n: Numeric[T]) =
    a.nonEmpty option {
      val arr = a.toArray
      Sorting.stableSort(arr)
      val size = arr.size
      val mid  = size / 2
      if (size % 2 == 0) n.toDouble(arr(mid) + arr(mid - 1)) / 2
      else n.toDouble(arr(mid))
    }

  def roundAt(n: Double, p: Int): BigDecimal = {
    BigDecimal(n).setScale(p, BigDecimal.RoundingMode.HALF_UP)
  }

  def closestMultipleOf(mult: Int, v: Int): Int =
    Math.round(v / mult.toFloat) * mult
}
