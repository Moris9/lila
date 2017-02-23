package lila.game

import scala.concurrent.duration._

import org.specs2.mutable._
import org.specs2.specification._

import lila.db.ByteArray

class BinaryClockHistoryTest extends Specification {

  val eps = 50.millis

  "binary clock history" should {

    "handle empty vectors" in {
      BinaryFormat.clockHistory.writeSide(0.millis, Vector.empty) must_== ByteArray.empty
    }

    "handle singleton vectors" in {
      val times = Vector(12345.millis)
      val bytes = BinaryFormat.clockHistory.writeSide(12345.millis, times)
      val restored = BinaryFormat.clockHistory.readSide(12345.millis, bytes, 1)

      restored.size must_== 1
      (restored(0) - times(0)).abs should be_<=(eps)
    }

    "restorable" in {
      val times = Vector(
        0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45, 48, 51, 54, 57, 60, 63,
        66, 69, 72, 75, 78, 81, 84, 87, 90, 93, 96, 99, 102, 105, 108, 199, 333, 567, 666, 2000, 30
      ).map(t => (2100 - t) * 100.millis)
      val bytes = BinaryFormat.clockHistory.writeSide(times.head, times)
      val restored = BinaryFormat.clockHistory.readSide(times.head, bytes, times.size)
      times.size must_== restored.size
      (restored, times).zipped.map(_ - _).forall(_.abs <= eps) should beTrue
    }

    "restore correspondence" in {
      val times = Vector(
        1181, 2040, 809, 1912, 750, 2305, 480, 2587
      ).map(t => 2.days - t.millis)
      val bytes = BinaryFormat.clockHistory.writeSide(2.days, times)
      val restored = BinaryFormat.clockHistory.readSide(2.days, bytes, times.size)
      times.size must_== restored.size
      println(times)
      println(restored)
      (restored, times).zipped.map(_ - _).forall(_.abs <= eps) should beTrue
    }
  }
}
