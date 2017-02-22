package lila.game

import scala.concurrent.duration._

import org.specs2.mutable._
import org.specs2.specification._

import lila.db.ByteArray

class BinaryMoveTimeTest extends Specification {

  val _0_ = "00000000"
  def write(c: Vector[Int]): List[String] =
    (BinaryFormat.moveTime write c.map(_ * 100 millis)).showBytes.split(',').toList
  def read(bytes: List[String]): Vector[Int] =
    BinaryFormat.moveTime read ByteArray.parseBytes(bytes) map (_.toTenths.toInt)

  "binary move times" should {
    "write" in {
      write(Vector(1, 10, 100, 5)) must_== {
        "00000010" :: "10100001" :: Nil
      }
      write(Vector(1, 10, 100, 5, 600)) must_== {
        "00000010" :: "10100001" :: "11110000" :: Nil
      }
    }
    "read" in {
      read("00000010" :: "10100001" :: Nil) must_== {
        Vector(1, 10, 100, 5)
      }
      read("00000010" :: "10100001" :: "11110000" :: Nil) must_== {
        Vector(1, 10, 100, 5, 600, 1)
      }
    }
    "buckets - long game" in {
      val times = Vector(
        0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45, 48, 51, 54, 57, 60, 63,
        66, 69, 72, 75, 78, 81, 84, 87, 90, 93, 96, 99, 102, 105, 108, 199, 333, 567, 666, 2000
      ).map(_ * 100 millis)
      val rounded = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(times))
      val expected = Vector(
        1, 1, 5, 10, 10, 15, 20, 20, 20, 30, 30, 30, 40, 40, 40, 40, 50, 50, 50, 60, 60, 60,
        60, 60, 80, 80, 80, 80, 80, 80, 80, 100, 100, 100, 100, 100, 100, 200, 300, 600, 600, 600
      ).map(_ * 100 millis)
      rounded must_== expected
      val again = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(rounded))
      again must_== rounded
    }
    "buckets - short game" in {
      val times = Vector(0, 3, 6, 9).map(_ * 100 millis)
      val rounded = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(times))
      val expected = Vector(1, 1, 5, 10).map(_ * 100 millis)
      rounded must_== expected
      val again = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(rounded))
      again must_== rounded
    }
    "buckets - short game - odd number of moves" in {
      val times = Vector(0, 3, 6).map(_ * 100 millis)
      val rounded = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(times))
      val expected = Vector(1, 1, 5).map(_ * 100 millis)
      rounded must_== expected
      val again = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(rounded))
      again must_== rounded
    }
  }
}
