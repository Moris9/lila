package lila.common

object Chronometer {

  case class Lap[A](result: A, nanos: Long) {

    def millis = (nanos / 1000000).toInt
    def micros = (nanos / 1000).toInt

    def logIfSlow(threshold: Int, logger: lila.log.Logger)(msg: A => String) = {
      if (millis >= threshold) logger.debug(s"<${millis}ms> ${msg(result)}")
      this
    }
  }

  case class FuLap[A](lap: Fu[Lap[A]]) {

    def logIfSlow(threshold: Int, logger: lila.log.Logger)(msg: A => String) = {
      lap.dforeach(_.logIfSlow(threshold, logger)(msg))
      this
    }

    def mon(path: lila.mon.RecPath) = {
      lap dforeach { l =>
        lila.mon.recPath(path)(l.nanos)
      }
      this
    }

    def pp: Fu[A] = lap dmap { l =>
      println(s"chrono ${l.micros} micros")
      l.result
    }

    def result = lap.dmap(_.result)
  }

  def apply[A](f: => Fu[A]): FuLap[A] = {
    val start = nowNanos
    FuLap(f dmap { Lap(_, nowNanos - start) })
  }

  def sync[A](f: => A): Lap[A] = {
    val start = nowNanos
    val res = f
    Lap(res, nowNanos - start)
  }

  def syncEffect[A](f: => A)(effect: Lap[A] => Unit): A = {
    val lap = sync(f)
    effect(lap)
    lap.result
  }
}
