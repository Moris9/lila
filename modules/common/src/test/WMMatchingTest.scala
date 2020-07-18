package lila.common
import org.specs2.mutable.Specification
import scala.util.{ Failure, Success }

object WMMatchingTest {
  def check(n: Int, a: Array[Int], res: (Int, Int)): Boolean = {
    val v           = Array.range(0, n)
    def f(x: Int)   = (x * (x + 1)) / 2
    def off(i: Int) = f(n - 1) - f(n - 1 - i)
    def pairScore(i: Int, j: Int): Option[Int] = {
      if (i > j) pairScore(j, i)
      else {
        val o = off(i) + (j - (i + 1))
        if (a(o) < 0) None else Some(a(o))
      }
    }
    def score(l: List[(Int, Int)]): (Int, Int) = (l.length, l.map(t => pairScore(t._1, t._2).head).sum)
    def checkScore(ans: (Int, Int)): Boolean   = ans._1 == res._1 && ans._2 == res._2
    val m                                      = WMMatching(v, pairScore)
    m match {
      case Success(l) => checkScore(score(l))
      case Failure(_) => false
    }
  }
}

class WMMatchingTest extends Specification {
  "precomputed tests" should {
    "data" in {

      WMMatchingTest.check(2, Array(7), (1, 7)) must beTrue
      WMMatchingTest.check(3, Array(-1, 20, -1), (1, 20)) must beTrue
      WMMatchingTest.check(3, Array(37, 13, 5), (1, 5)) must beTrue
      WMMatchingTest.check(4, Array(-1, -1, -1, -1, 46, -1), (1, 46)) must beTrue
      WMMatchingTest.check(4, Array(6, -1, 22, 25, -1, -1), (2, 47)) must beTrue
      WMMatchingTest.check(4, Array(34, 67, 30, 35, 20, 0), (2, 34)) must beTrue
      WMMatchingTest.check(5, Array(-1, -1, 16, -1, -1, -1, -1, -1, -1, -1), (1, 16)) must beTrue
      WMMatchingTest.check(5, Array(-1, -1, -1, -1, -1, 9, -1, 74, -1, -1), (1, 9)) must beTrue
      WMMatchingTest.check(5, Array(-1, -1, -1, -1, 48, 42, 70, 47, -1, 95), (2, 117)) must beTrue
      WMMatchingTest.check(5, Array(71, 46, 31, 61, 42, 13, 20, 98, 27, 16), (2, 40)) must beTrue
      WMMatchingTest.check(
        6,
        Array(
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 94, -1
        ),
        (1, 94)
      ) must beTrue
      WMMatchingTest.check(
        6,
        Array(
          -1, -1, -1, -1, -1, -1, -1, 51, -1, -1, 2, -1, -1, 89, -1
        ),
        (2, 91)
      ) must beTrue
      WMMatchingTest.check(
        6,
        Array(
          -1, -1, 67, 92, 11, -1, 37, -1, 70, -1, -1, -1, 29, -1, 10
        ),
        (2, 40)
      ) must beTrue
      WMMatchingTest.check(
        6,
        Array(
          91, 45, 1, 43, 24, 17, 28, 38, 52, 26, 47, 9, 0, 54, 62
        ),
        (3, 41)
      ) must beTrue
      WMMatchingTest.check(
        7,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, 42, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        (1, 42)
      ) must beTrue
      WMMatchingTest.check(
        7,
        Array(12, -1, -1, -1, -1, -1, -1, -1, -1, -1, 92, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        (1, 12)
      ) must beTrue
      WMMatchingTest.check(
        7,
        Array(44, -1, -1, 32, -1, -1, -1, -1, 41, -1, -1, -1, -1, 15, -1, 85, -1, -1, -1, -1, -1),
        (3, 144)
      ) must beTrue
      WMMatchingTest.check(
        7,
        Array(-1, -1, -1, 14, 32, 89, -1, -1, 26, -1, -1, -1, 3, 57, -1, 86, -1, 85, 93, -1, 41),
        (3, 120)
      ) must beTrue
      WMMatchingTest.check(
        7,
        Array(93, 13, 67, 27, 76, 20, 42, 11, 49, 42, 39, 72, 38, 9, 14, 49, 11, 94, 59, 20, 15),
        (3, 39)
      ) must beTrue
      WMMatchingTest.check(
        8,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1),
        (1, 10)
      ) must beTrue
      WMMatchingTest.check(
        8,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 7, -1, 54, -1, -1, -1, -1,
          -1, -1, -1, -1),
        (2, 61)
      ) must beTrue
      WMMatchingTest.check(
        8,
        Array(-1, -1, -1, 51, -1, 99, -1, -1, -1, -1, -1, 96, 60, -1, -1, -1, -1, 72, -1, -1, -1, 96, -1, -1,
          -1, 24, -1, -1),
        (3, 135)
      ) must beTrue
      WMMatchingTest.check(
        8,
        Array(83, 26, 40, -1, -1, -1, -1, -1, 96, 17, -1, 56, 45, 9, -1, -1, -1, -1, 10, 78, 93, 88, 5, 15,
          -1, -1, -1, -1),
        (4, 164)
      ) must beTrue
      WMMatchingTest.check(
        8,
        Array(14, 98, 5, 28, 55, 74, 53, 40, 44, 85, 30, 79, 68, 16, 85, 68, 91, 36, 48, 36, 65, 5, 6, 21, 18,
          42, 31, 84),
        (4, 82)
      ) must beTrue
      WMMatchingTest.check(
        9,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, 78, -1, -1, -1, -1, -1),
        (1, 78)
      ) must beTrue
      WMMatchingTest.check(
        9,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, 41, -1, -1, -1, -1, -1, -1, -1, -1, -1, 40, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, 43, -1, -1, -1, -1),
        (2, 83)
      ) must beTrue
      WMMatchingTest.check(
        9,
        Array(12, 46, -1, 64, -1, 53, 30, -1, -1, -1, -1, -1, 6, -1, -1, -1, -1, 12, -1, -1, -1, -1, -1, -1,
          -1, -1, 97, -1, -1, 26, -1, -1, -1, -1, -1, -1),
        (4, 74)
      ) must beTrue
      WMMatchingTest.check(
        9,
        Array(-1, 30, 66, -1, 53, -1, -1, -1, 69, 35, -1, -1, 51, 25, -1, -1, 23, -1, -1, 89, 83, -1, -1, 69,
          91, 95, -1, -1, 8, 19, 69, -1, -1, 81, -1, 28),
        (4, 139)
      ) must beTrue
      WMMatchingTest.check(
        9,
        Array(72, 38, 28, 39, 83, 12, 53, 19, 15, 19, 25, 97, 41, 83, 78, 70, 6, 68, 83, 0, 90, 25, 46, 55,
          72, 91, 79, 61, 56, 13, 69, 19, 69, 43, 46, 28),
        (4, 44)
      ) must beTrue
      WMMatchingTest.check(
        10,
        Array(
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, 4, -1, 90, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        ),
        (1, 4)
      ) must beTrue
      WMMatchingTest.check(
        10,
        Array(
          -1, -1, -1, -1, -1, 61, -1, -1, 39, -1, -1, -1, -1, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, 70, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        ),
        (2, 44)
      ) must beTrue
      WMMatchingTest.check(
        10,
        Array(
          -1, -1, 51, 79, -1, -1, 18, -1, 14, 22, -1, -1, -1, 20, -1, -1, -1, -1, 23, 13, -1, 90, -1, -1, -1,
          -1, -1, 43, -1, 89, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        ),
        (4, 90)
      ) must beTrue
      WMMatchingTest.check(
        10,
        Array(
          -1, 13, 12, -1, -1, 0, 58, 73, -1, 11, -1, 40, 42, 31, 16, -1, -1, -1, -1, -1, -1, 7, 32, 7, -1, 80,
          -1, 92, -1, -1, -1, 42, -1, -1, -1, 6, 49, -1, 74, -1, 99, -1, 40, -1, 95
        ),
        (5, 105)
      ) must beTrue
      WMMatchingTest.check(
        10,
        Array(
          58, 5, 69, 21, 44, 81, 81, 18, 59, 82, 6, 36, 33, 9, 64, 66, 20, 26, 18, 21, 45, 54, 5, 79, 68, 18,
          55, 82, 57, 80, 75, 71, 85, 82, 4, 1, 26, 24, 65, 25, 85, 31, 2, 53, 87
        ),
        (5, 18)
      ) must beTrue
      WMMatchingTest.check(
        11,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, 77, -1, -1, -1, -1),
        (2, 79)
      ) must beTrue
      WMMatchingTest.check(
        11,
        Array(-1, -1, 87, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 96, -1,
          -1, -1, 77, -1, -1, -1, -1, -1, -1, -1, 93, -1, 65, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1),
        (3, 229)
      ) must beTrue
      WMMatchingTest.check(
        11,
        Array(-1, -1, -1, 99, -1, -1, 84, -1, -1, -1, 5, 73, -1, 10, -1, -1, -1, 5, -1, 43, -1, -1, -1, 97,
          -1, -1, 34, -1, 26, -1, 27, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 28, -1, -1, -1, -1,
          -1, -1, 3, -1, -1, -1),
        (4, 133)
      ) must beTrue
      WMMatchingTest.check(
        11,
        Array(15, -1, -1, -1, 31, 41, -1, -1, 42, 15, -1, 69, 34, -1, -1, 50, -1, 99, -1, 63, -1, 84, -1, 17,
          66, -1, -1, -1, -1, -1, 73, 32, 36, 99, 68, -1, -1, -1, -1, -1, -1, 29, 4, -1, -1, -1, -1, 83, -1,
          43, 43, 61, 95, 47, 21),
        (5, 106)
      ) must beTrue
      WMMatchingTest.check(
        11,
        Array(58, 50, 92, 71, 11, 53, 16, 8, 19, 34, 40, 13, 57, 80, 79, 66, 82, 42, 54, 52, 8, 82, 81, 23,
          59, 84, 28, 61, 59, 67, 23, 0, 24, 42, 42, 96, 94, 34, 99, 96, 16, 7, 10, 44, 15, 84, 52, 67, 99,
          52, 7, 63, 8, 9, 75),
        (5, 48)
      ) must beTrue
      WMMatchingTest.check(
        12,
        Array(-1, -1, -1, -1, -1, 92, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 22, -1, -1, -1, 88, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        (2, 114)
      ) must beTrue
      WMMatchingTest.check(
        12,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, 55, -1, -1, 39, -1, -1, -1, -1, 76, -1, -1, -1, -1, -1, -1, -1, -1, -1, 84,
          -1, -1, -1, -1, -1, -1, -1, 24, -1, -1, -1, -1, -1, -1, -1, -1, 88),
        (4, 251)
      ) must beTrue
      WMMatchingTest.check(
        12,
        Array(34, -1, 24, -1, -1, 47, 29, -1, -1, 4, -1, 20, -1, -1, -1, -1, -1, -1, -1, -1, 97, -1, 11, -1,
          -1, -1, -1, -1, 84, -1, -1, -1, 91, -1, -1, -1, 19, -1, 62, -1, -1, -1, -1, -1, -1, -1, -1, 45, -1,
          -1, -1, 33, 64, 16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        (6, 217)
      ) must beTrue
      WMMatchingTest.check(
        12,
        Array(-1, 65, 10, -1, -1, -1, -1, -1, 93, -1, -1, 58, -1, 70, -1, 21, 79, 77, 74, 59, 2, 67, -1, -1,
          88, -1, 79, -1, 38, 34, -1, -1, -1, 25, 54, -1, 4, -1, 27, 50, 92, -1, -1, 64, 98, -1, 49, -1, -1,
          84, -1, 10, 29, 50, -1, 52, -1, 43, -1, -1, 55, 2, -1, -1, -1, -1),
        (6, 137)
      ) must beTrue
      WMMatchingTest.check(
        12,
        Array(43, 17, 78, 32, 36, 23, 73, 52, 65, 58, 32, 0, 80, 46, 71, 1, 16, 89, 81, 38, 6, 57, 51, 0, 18,
          67, 40, 12, 88, 74, 24, 67, 65, 94, 44, 44, 48, 3, 0, 97, 83, 46, 47, 47, 75, 83, 94, 45, 23, 45,
          60, 59, 20, 89, 71, 90, 92, 76, 35, 51, 96, 26, 42, 70, 14, 6),
        (6, 80)
      ) must beTrue
      WMMatchingTest.check(
        13,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 92, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 82, -1),
        (2, 85)
      ) must beTrue
      WMMatchingTest.check(
        13,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, 43, -1, 59, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 49, -1, -1, -1, -1,
          32, 39, -1, -1, -1, -1, -1, -1, -1, -1, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, 26),
        (4, 133)
      ) must beTrue
      WMMatchingTest.check(
        13,
        Array(-1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, 19, -1, -1, -1, -1, -1, -1, 26, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, 8, -1, -1, 18, -1, -1, -1, -1, -1, 1, -1, 70, 94, -1, -1, -1, -1, -1, 80, -1, -1,
          90, 77, -1, -1, 86, -1, -1, -1, -1, -1, -1, -1, 45, -1, -1, 60, 71, -1, -1, -1, 3, -1, -1, 33, 62,
          -1, 70, -1, -1),
        (6, 266)
      ) must beTrue
      WMMatchingTest.check(
        13,
        Array(-1, -1, -1, 44, 37, -1, -1, 0, 3, 48, 29, -1, 34, -1, 99, 15, -1, -1, 11, -1, 31, 44, 49, -1,
          -1, -1, 58, 65, 56, -1, -1, 81, -1, 27, 50, -1, 93, -1, -1, -1, -1, -1, -1, -1, 11, -1, 5, 14, 95,
          20, 4, -1, 38, -1, 85, 29, -1, -1, -1, -1, 47, -1, 53, 25, -1, -1, 55, 86, -1, -1, -1, 44, 10, -1,
          85, 81, -1, 81),
        (6, 122)
      ) must beTrue
      WMMatchingTest.check(
        13,
        Array(84, 50, 9, 33, 33, 74, 99, 65, 80, 95, 70, 66, 4, 9, 41, 76, 28, 23, 55, 70, 96, 56, 20, 0, 53,
          37, 81, 56, 26, 1, 94, 93, 34, 57, 83, 4, 14, 67, 85, 24, 55, 77, 96, 50, 90, 85, 9, 2, 98, 77, 10,
          6, 3, 62, 92, 44, 74, 53, 23, 31, 82, 71, 45, 6, 4, 82, 8, 54, 55, 6, 28, 54, 78, 18, 59, 58, 83,
          75),
        (6, 38)
      ) must beTrue
      WMMatchingTest.check(
        14,
        Array(-1, -1, -1, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 82, -1, -1, -1, -1, -1,
          29, -1, -1, 65, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        (3, 125)
      ) must beTrue
      WMMatchingTest.check(
        14,
        Array(-1, -1, 81, -1, -1, 25, -1, 96, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, 56, -1, -1, -1, -1, -1, 60, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 95, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 50,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 45, -1, -1, -1, 69, -1),
        (4, 230)
      ) must beTrue
      WMMatchingTest.check(
        14,
        Array(-1, -1, -1, -1, -1, -1, 74, -1, -1, -1, -1, -1, 45, -1, -1, 93, -1, -1, -1, -1, -1, -1, -1, -1,
          95, -1, -1, -1, -1, 44, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 82, -1, -1, 89, -1, 42, -1, -1, 46,
          -1, -1, -1, -1, -1, 94, -1, -1, -1, 32, 60, -1, 76, 92, -1, -1, 45, -1, -1, -1, 77, 54, -1, -1, -1,
          62, -1, -1, -1, -1, 91, -1, -1, -1, 71, -1, -1, -1, 45, 74, -1, -1),
        (7, 443)
      ) must beTrue
      WMMatchingTest.check(
        14,
        Array(-1, 56, -1, 89, -1, 75, -1, 44, -1, 23, -1, -1, -1, 59, -1, -1, 99, 18, 45, -1, 71, -1, 96, -1,
          -1, -1, -1, 90, -1, 29, -1, -1, -1, 17, 35, 10, 30, -1, -1, -1, -1, -1, -1, -1, 13, 46, 43, 96, -1,
          -1, -1, 58, 83, -1, 59, 12, -1, 15, 47, 97, -1, -1, 43, 30, 93, -1, 3, -1, 17, 33, 54, 41, 26, -1,
          -1, 84, 72, -1, 25, 2, -1, 26, -1, 32, 41, -1, -1, 63, -1, -1, -1),
        (7, 170)
      ) must beTrue
      WMMatchingTest.check(
        14,
        Array(94, 48, 72, 88, 6, 60, 20, 41, 17, 87, 12, 44, 72, 67, 58, 91, 1, 72, 4, 83, 57, 44, 22, 19, 93,
          70, 32, 73, 61, 44, 39, 55, 56, 30, 9, 90, 20, 70, 82, 44, 34, 67, 13, 24, 25, 85, 4, 62, 15, 40,
          21, 30, 30, 40, 30, 28, 89, 22, 17, 8, 82, 56, 28, 49, 89, 55, 3, 55, 18, 41, 75, 10, 16, 18, 9, 60,
          23, 17, 28, 6, 69, 84, 37, 88, 95, 93, 16, 42, 66, 49, 7),
        (7, 92)
      ) must beTrue
      WMMatchingTest.check(
        15,
        Array(
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 18, -1,
          -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 77, -1, -1, -1, -1, -1, 44, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 66, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1
        ),
        (4, 161)
      ) must beTrue
      WMMatchingTest.check(
        15,
        Array(
          31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 10, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, 9, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 59, -1, -1, -1, -1, -1, -1, 57, 96, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 44, -1, -1, 49,
          -1, -1, -1, -1, 11
        ),
        (6, 207)
      ) must beTrue
      WMMatchingTest.check(
        15,
        Array(
          -1, -1, 96, -1, -1, 92, -1, 77, -1, -1, -1, -1, -1, 46, -1, -1, -1, 53, 41, 93, 26, 62, -1, 3, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, 50, -1, -1, -1, -1, -1, -1, 53, -1, -1, -1, -1, -1, -1, -1, 54, -1,
          54, -1, -1, 56, -1, 54, -1, 26, 62, -1, -1, -1, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 35,
          -1, 51, -1, 70, -1, 58, -1, -1, -1, -1, -1, -1, 32, 34, -1, -1, -1, -1, -1, -1, -1, -1, 24, -1, -1,
          -1, -1, -1, -1, -1
        ),
        (7, 247)
      ) must beTrue
      WMMatchingTest.check(
        15,
        Array(
          10, -1, -1, 74, 49, 70, 48, 51, 45, -1, -1, 96, -1, -1, -1, 61, -1, -1, -1, -1, 0, 20, 69, -1, -1,
          17, -1, 76, -1, 91, 84, 53, -1, -1, -1, 76, -1, 43, 14, 74, 70, 17, 27, -1, 27, -1, -1, 31, 95, 94,
          -1, -1, -1, 3, -1, -1, -1, 15, -1, 73, 27, -1, 67, -1, -1, -1, 50, 52, -1, 53, 73, -1, -1, -1, 90,
          61, -1, 54, 71, -1, 52, -1, -1, -1, -1, -1, -1, 57, -1, 95, 79, -1, -1, 64, -1, 52, 8, -1, -1, 18,
          -1, 20, -1, 2, 62
        ),
        (7, 138)
      ) must beTrue
      WMMatchingTest.check(
        15,
        Array(
          13, 33, 96, 41, 38, 93, 20, 88, 69, 10, 87, 91, 56, 22, 87, 8, 88, 52, 53, 82, 19, 31, 90, 23, 59,
          89, 67, 29, 92, 44, 8, 86, 59, 17, 62, 30, 74, 32, 62, 41, 80, 97, 55, 0, 96, 57, 55, 85, 62, 35,
          68, 68, 35, 41, 47, 73, 89, 38, 26, 44, 17, 56, 29, 76, 96, 45, 9, 2, 91, 29, 31, 5, 17, 88, 15, 69,
          30, 30, 20, 8, 0, 93, 74, 48, 67, 56, 21, 52, 97, 68, 39, 0, 33, 38, 80, 57, 54, 76, 45, 76, 18, 77,
          47, 93, 11
        ),
        (7, 49)
      ) must beTrue
      WMMatchingTest.check(
        16,
        Array(
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 72, -1, -1, -1, -1, -1, 72, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 47, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 33, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 59, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 48, -1, -1
        ),
        (5, 259)
      ) must beTrue
      WMMatchingTest.check(
        16,
        Array(
          -1, -1, -1, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 66, -1, 67, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, 43, 61, -1, -1, -1, -1, -1, -1, -1, 29, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, 58, -1, -1, -1, -1, 70, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 12, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 34, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 76, -1, -1, -1, -1, 63, -1, -1, -1
        ),
        (7, 324)
      ) must beTrue
      WMMatchingTest.check(
        16,
        Array(
          -1, -1, -1, -1, -1, 54, 48, -1, 53, -1, -1, -1, -1, 22, -1, -1, -1, 26, 88, -1, -1, -1, 7, 49, -1,
          -1, 19, -1, 23, 34, -1, -1, 36, 0, -1, 32, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, 76, -1, -1, -1, -1, 36, -1, 51, -1, -1, -1, 4, -1, 29, -1, -1, -1, -1, -1, 87, 9,
          41, -1, -1, 9, -1, -1, -1, -1, -1, 53, 41, -1, -1, -1, 60, -1, -1, -1, 84, -1, -1, -1, -1, 98, 8,
          -1, -1, -1, -1, -1, -1, -1, 69, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        ),
        (7, 184)
      ) must beTrue
      WMMatchingTest.check(
        16,
        Array(
          -1, 99, -1, -1, -1, -1, 83, -1, -1, -1, 56, 89, 60, -1, 23, -1, 10, 15, -1, 4, 82, 28, 50, 12, 24,
          13, -1, 12, -1, 59, 7, 7, 3, -1, -1, -1, 4, 48, -1, 43, -1, 53, -1, 72, 55, -1, 79, -1, -1, -1, 30,
          -1, 66, -1, -1, 19, -1, -1, 7, 75, -1, -1, 77, -1, -1, -1, 1, 8, 83, 83, 49, 12, -1, 42, -1, 89, 59,
          -1, 47, 88, 57, 40, -1, 41, -1, -1, -1, 90, -1, 99, -1, -1, -1, -1, -1, -1, 82, -1, 2, 7, -1, -1,
          -1, 95, 89, 18, -1, -1, -1, -1, 52, 3, -1, -1, 74, 48, -1, 36, -1, -1
        ),
        (8, 133)
      ) must beTrue
      WMMatchingTest.check(
        16,
        Array(
          16, 25, 65, 79, 47, 99, 30, 26, 88, 74, 96, 62, 22, 39, 37, 25, 76, 79, 61, 88, 12, 24, 19, 52, 40,
          2, 66, 3, 18, 62, 72, 42, 77, 69, 26, 67, 24, 50, 10, 21, 52, 76, 83, 18, 73, 50, 43, 48, 72, 41,
          39, 9, 0, 36, 97, 74, 15, 8, 60, 4, 75, 20, 0, 11, 3, 64, 97, 77, 84, 52, 44, 70, 47, 28, 92, 1, 23,
          87, 55, 15, 97, 8, 52, 89, 82, 19, 15, 33, 97, 5, 98, 34, 32, 20, 23, 70, 74, 30, 62, 75, 75, 22, 9,
          55, 7, 30, 54, 50, 50, 25, 69, 31, 53, 54, 84, 17, 53, 65, 20, 0
        ),
        (8, 81)
      ) must beTrue
      WMMatchingTest.check(
        17,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 31, -1, -1, -1, -1,
          -1, -1, 81, -1, -1, -1, -1, -1, -1, 3, -1, -1, -1, -1, 53, -1, -1, -1, -1, -1, -1, -1, -1, -1, 94,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          85, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        (4, 200)
      ) must beTrue
      WMMatchingTest.check(
        17,
        Array(-1, -1, -1, -1, 70, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 47, -1,
          -1, -1, -1, -1, -1, -1, -1, 85, -1, -1, -1, -1, 42, -1, -1, -1, -1, -1, -1, 25, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 4, -1, -1, 74, -1, -1, -1, 3, -1, -1,
          -1, 24, -1, -1, -1, -1, -1, -1, -1, -1, 7, -1, -1, 58, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 4, -1, -1, -1, -1, -1, -1, -1, 11, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        (6, 90)
      ) must beTrue
      WMMatchingTest.check(
        17,
        Array(82, 5, -1, -1, 47, -1, -1, -1, -1, 73, -1, 99, -1, -1, 75, -1, -1, -1, -1, -1, 60, -1, -1, -1,
          22, 61, -1, -1, -1, -1, -1, 27, -1, -1, 67, -1, -1, -1, -1, -1, 38, -1, -1, -1, 16, -1, -1, -1, -1,
          -1, 61, -1, -1, -1, 60, -1, -1, 15, -1, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, 70, -1, -1, 26, 58, -1, -1, 94, -1, -1, -1, -1, -1, -1, -1, 25, -1, -1, -1, -1, -1, 89, -1, -1,
          -1, 14, -1, -1, -1, -1, -1, 65, 74, -1, -1, -1, -1, -1, -1, 48, -1, -1, -1, 48, 24, -1, -1, 13, -1,
          13, 62, -1, 37, -1, -1, 19, -1, -1, -1, -1, -1),
        (8, 164)
      ) must beTrue
      WMMatchingTest.check(
        17,
        Array(-1, -1, 43, 57, 4, 59, 2, -1, 37, -1, 60, 61, 5, -1, -1, -1, -1, -1, -1, 2, -1, -1, -1, -1, -1,
          -1, -1, -1, 0, 9, -1, -1, -1, 90, 93, -1, -1, 49, 73, 50, 91, 3, -1, -1, 31, 97, 22, 58, 97, -1, 49,
          2, 34, -1, 73, 19, -1, 35, -1, -1, -1, 29, 5, -1, 16, -1, -1, 78, 29, 0, -1, -1, 54, 54, 77, -1, -1,
          -1, -1, -1, -1, 63, -1, 13, -1, -1, 13, 36, 93, -1, -1, 65, 20, 47, 29, -1, 49, 94, -1, 32, 26, 19,
          21, 30, -1, -1, -1, -1, -1, -1, 8, -1, 81, -1, 29, -1, -1, -1, 81, 9, 45, -1, -1, -1, 35, 78, -1,
          -1, 92, -1, 7, -1, 56, 97, -1, 63),
        (8, 63)
      ) must beTrue
      WMMatchingTest.check(
        17,
        Array(29, 48, 66, 72, 57, 98, 86, 50, 85, 53, 7, 1, 32, 88, 67, 53, 36, 74, 67, 65, 66, 3, 23, 21, 23,
          92, 13, 74, 92, 68, 56, 84, 12, 41, 36, 97, 97, 85, 11, 71, 62, 99, 36, 2, 23, 92, 42, 82, 33, 97,
          21, 14, 88, 77, 95, 22, 43, 43, 23, 42, 89, 4, 90, 44, 18, 95, 19, 14, 29, 91, 7, 24, 41, 29, 22,
          16, 6, 97, 22, 21, 45, 30, 43, 6, 56, 80, 65, 11, 39, 22, 56, 98, 9, 97, 27, 54, 59, 79, 41, 18, 37,
          70, 95, 87, 42, 10, 15, 23, 25, 1, 33, 50, 77, 69, 24, 49, 50, 67, 52, 76, 98, 6, 10, 59, 27, 29,
          41, 87, 54, 39, 69, 82, 10, 55, 19, 23),
        (8, 42)
      ) must beTrue
      WMMatchingTest.check(
        18,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 25, -1, -1, -1, -1, 67, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 34, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          69, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 68, -1, -1, -1, 54, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, 56, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1),
        (4, 169)
      ) must beTrue
      WMMatchingTest.check(
        18,
        Array(-1, 93, -1, 35, -1, -1, -1, 73, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 92, -1, -1, 39, -1, -1, -1, -1, -1, -1, 97, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          60, -1, -1, 66, -1, -1, -1, -1, -1, -1, 37, -1, -1, -1, -1, 16, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1,
          -1, -1, -1, -1, -1, -1, 77, -1, -1, -1, -1, -1, -1, -1, -1, 44, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, 1, -1, 11),
        (6, 253)
      ) must beTrue
      WMMatchingTest.check(
        18,
        Array(-1, -1, -1, 57, -1, 67, -1, -1, -1, -1, 30, 73, -1, 7, 78, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, 74, -1, -1, 54, -1, 9, -1, -1, -1, 72, -1, -1, -1, -1, -1, 21, -1, 89, -1, -1, -1,
          -1, -1, -1, -1, 92, -1, -1, 66, 59, -1, -1, -1, -1, 63, -1, -1, -1, -1, -1, 74, -1, -1, 10, -1, 29,
          -1, -1, -1, -1, 57, -1, -1, -1, -1, -1, -1, -1, -1, 95, -1, -1, -1, 91, -1, -1, -1, -1, -1, 97, 87,
          -1, -1, -1, -1, -1, 61, -1, 49, -1, -1, 84, -1, 0, 35, -1, -1, -1, -1, -1, 33, -1, -1, -1, -1, -1,
          -1, 69, -1, -1, -1, -1, -1, -1, -1, -1, -1, 91, -1, -1, -1, -1, -1, 9, 70, 44, 51, 87, -1, -1, -1,
          -1, 0, -1, -1),
        (9, 424)
      ) must beTrue
      WMMatchingTest.check(
        18,
        Array(-1, 46, 68, 59, -1, 68, -1, -1, -1, -1, 89, -1, 21, 92, -1, 12, 40, -1, -1, -1, 39, -1, 84, -1,
          -1, 38, 16, -1, 29, 39, 38, -1, 9, 68, 44, -1, -1, -1, -1, 77, -1, 88, -1, 35, -1, 43, -1, -1, 46,
          -1, -1, -1, -1, 89, 99, -1, 96, 58, -1, 67, -1, 65, 55, -1, 8, 50, -1, -1, -1, 51, -1, -1, 14, 12,
          87, 90, -1, 49, -1, -1, 73, 24, -1, -1, -1, -1, 76, -1, 89, -1, 3, -1, 68, 89, -1, 79, -1, 30, -1,
          -1, 29, -1, 93, 83, 61, -1, -1, 55, -1, -1, -1, -1, -1, 69, 34, 82, 47, 98, 11, -1, 39, 18, 85, -1,
          76, -1, -1, 15, 30, -1, 41, 13, -1, -1, -1, -1, -1, -1, 37, 30, -1, 93, 98, 25, 40, -1, -1, 15, -1,
          67, 84, -1, -1),
        (9, 194)
      ) must beTrue
      WMMatchingTest.check(
        18,
        Array(10, 75, 28, 0, 99, 6, 10, 59, 63, 19, 91, 96, 78, 46, 79, 63, 93, 76, 24, 68, 92, 55, 41, 42,
          94, 91, 57, 46, 21, 69, 61, 2, 21, 86, 68, 81, 47, 6, 1, 52, 1, 36, 38, 43, 83, 37, 1, 19, 49, 83,
          70, 19, 79, 19, 98, 12, 88, 96, 32, 76, 56, 42, 67, 35, 1, 92, 12, 35, 44, 68, 21, 66, 36, 78, 59,
          53, 58, 12, 71, 63, 0, 0, 79, 12, 71, 22, 51, 92, 49, 80, 0, 44, 36, 28, 40, 98, 33, 9, 70, 76, 71,
          17, 58, 80, 15, 59, 92, 86, 51, 11, 45, 71, 91, 17, 43, 15, 16, 12, 4, 64, 0, 75, 61, 30, 43, 20,
          67, 58, 49, 85, 51, 10, 35, 13, 1, 77, 99, 19, 12, 94, 43, 22, 92, 55, 30, 71, 8, 43, 16, 89, 36,
          29, 37),
        (9, 52)
      ) must beTrue
      WMMatchingTest.check(
        19,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 33, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 12, -1, -1, 56, -1, -1, -1, -1, -1, 49, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, 95, -1, -1, -1, -1, -1, 70, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 60),
        (5, 154)
      ) must beTrue
      WMMatchingTest.check(
        19,
        Array(53, -1, -1, 42, -1, -1, -1, -1, -1, -1, -1, -1, 37, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 58,
          -1, -1, -1, -1, -1, -1, 12, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 90, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 50, -1, -1, 22, -1, -1, -1, 33, -1, 48, -1,
          24, -1, -1, -1, -1, 98, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 91, -1, -1,
          -1, -1, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 38, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, 92, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 65, -1, -1),
        (8, 378)
      ) must beTrue
      WMMatchingTest.check(
        19,
        Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 76, 84, -1, -1, -1, 49,
          78, -1, 76, -1, -1, -1, -1, -1, 98, -1, -1, -1, 29, -1, -1, -1, 10, -1, 45, -1, 98, 29, -1, 7, -1,
          -1, -1, -1, -1, -1, -1, -1, 62, 56, 62, -1, -1, -1, -1, -1, 56, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          41, -1, -1, -1, -1, -1, 27, -1, -1, -1, -1, 40, -1, -1, -1, -1, -1, 94, -1, -1, -1, -1, 4, -1, -1,
          52, 98, -1, -1, -1, -1, -1, 53, -1, -1, -1, -1, -1, 96, -1, 47, -1, 79, 36, -1, -1, -1, -1, 78, 3,
          -1, -1, -1, -1, -1, 39, -1, -1, -1, -1, -1, -1, -1, -1, 8, -1, -1, -1, -1, -1, -1, 6, 41, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, 11, -1, 76, -1, -1, -1, 58, 54, -1, 80, 69, 4, -1, 2),
        (9, 331)
      ) must beTrue
      WMMatchingTest.check(
        19,
        Array(-1, -1, -1, 62, 93, 59, 75, -1, -1, 86, 74, -1, 88, 19, -1, -1, 4, 96, 45, 23, -1, 95, -1, 27,
          -1, -1, -1, -1, 54, 25, -1, 71, 68, 77, -1, -1, -1, 84, -1, -1, 90, -1, 30, 77, -1, 62, 41, -1, -1,
          31, -1, -1, 95, -1, 4, -1, 22, 47, 80, 54, -1, 72, -1, 46, 4, -1, -1, 32, -1, -1, 88, -1, 9, -1, 84,
          96, -1, 47, 71, -1, 82, 52, 40, 96, -1, 25, 66, 12, 32, -1, -1, 4, -1, 93, -1, -1, 44, -1, 23, -1,
          -1, -1, -1, -1, 60, 59, -1, -1, 82, -1, -1, -1, 16, 93, -1, 79, -1, 15, 3, 83, -1, 20, -1, 48, 39,
          28, -1, -1, -1, -1, 37, -1, 42, -1, -1, 28, -1, -1, -1, 81, -1, -1, 96, -1, 96, -1, 36, -1, -1, -1,
          -1, 79, 78, -1, 1, -1, 15, 31, -1, 96, 81, -1, -1, -1, 56, 35, 57, -1, -1, -1, 65),
        (9, 176)
      ) must beTrue
      WMMatchingTest.check(
        19,
        Array(79, 63, 11, 12, 27, 79, 41, 1, 82, 83, 80, 32, 2, 73, 59, 69, 78, 63, 71, 1, 38, 13, 95, 89, 25,
          7, 60, 33, 33, 79, 8, 21, 92, 4, 96, 84, 63, 96, 49, 5, 91, 93, 76, 69, 16, 59, 20, 5, 69, 8, 18,
          41, 82, 64, 6, 82, 6, 98, 39, 53, 46, 72, 91, 55, 95, 58, 3, 68, 25, 70, 64, 27, 31, 94, 13, 46, 33,
          11, 67, 44, 94, 67, 98, 92, 42, 34, 96, 66, 31, 80, 52, 15, 28, 35, 74, 22, 44, 73, 31, 16, 97, 53,
          21, 2, 62, 18, 98, 3, 72, 16, 48, 22, 72, 2, 81, 6, 93, 58, 9, 20, 25, 92, 59, 20, 74, 7, 55, 59,
          64, 1, 32, 62, 13, 86, 45, 36, 83, 3, 17, 21, 84, 89, 6, 52, 21, 4, 21, 1, 59, 75, 95, 27, 80, 63,
          95, 75, 1, 99, 83, 97, 67, 0, 35, 51, 20, 30, 17, 7, 71, 47, 3),
        (9, 20)
      ) must beTrue
      WMMatchingTest.check(
        20,
        Array(29, -1, -1, -1, 48, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 11, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 55, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 82, -1, -1, -1, -1, -1, -1, -1, 17, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 12, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, 75, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 28, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        (5, 141)
      ) must beTrue
      WMMatchingTest.check(
        20,
        Array(-1, 12, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 99, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 47, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 59, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 75,
          -1, -1, -1, -1, -1, 64, -1, -1, 82, -1, -1, -1, 72, -1, -1, -1, -1, -1, -1, -1, -1, -1, 13, -1, -1,
          -1, 95, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 74, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 38, -1, -1, 59, -1, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, 76, 47, -1, -1, -1, -1, -1, -1, -1, -1, 47, -1, -1, -1, -1, -1, -1, 59, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 92, -1, -1, -1, -1),
        (9, 510)
      ) must beTrue
      WMMatchingTest.check(
        20,
        Array(35, 20, -1, 44, -1, -1, -1, -1, -1, 15, -1, -1, -1, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, 7,
          -1, -1, -1, -1, -1, -1, -1, 25, -1, 35, -1, -1, -1, -1, 79, 31, -1, 29, 84, -1, -1, -1, -1, -1, -1,
          87, -1, -1, 98, 53, 73, 27, -1, -1, 2, -1, 13, -1, -1, -1, -1, -1, -1, -1, 12, -1, -1, -1, -1, -1,
          46, -1, -1, -1, -1, 96, -1, -1, -1, -1, -1, -1, -1, 37, 17, -1, -1, -1, 45, -1, 92, -1, -1, -1, -1,
          5, -1, -1, -1, -1, -1, -1, -1, 75, -1, -1, 84, -1, -1, 77, -1, -1, -1, -1, 91, -1, -1, 68, -1, -1,
          -1, -1, -1, -1, -1, 13, -1, -1, 23, -1, -1, -1, 2, -1, -1, -1, -1, -1, 88, -1, -1, -1, 32, -1, -1,
          43, -1, -1, -1, -1, -1, 2, -1, 19, -1, -1, -1, 78, -1, 57, -1, -1, -1, -1, -1, -1, 64, -1, -1, -1,
          -1, 19, 86, 49, -1, -1, -1, -1, -1, -1, -1, -1, -1, 25, -1, -1),
        (10, 234)
      ) must beTrue
      WMMatchingTest.check(
        20,
        Array(-1, -1, -1, -1, 32, -1, -1, 40, -1, -1, -1, -1, 91, -1, -1, 49, -1, 74, 37, -1, -1, -1, -1, -1,
          -1, 82, -1, -1, 20, 3, -1, -1, -1, 82, 67, 39, -1, 1, 8, 26, 54, -1, -1, 6, -1, -1, 7, -1, 28, 6,
          -1, -1, 95, -1, 16, -1, -1, 11, 14, 82, 8, 41, 4, 76, -1, -1, 49, -1, 35, 66, 50, 24, -1, 65, -1,
          -1, 34, 70, 55, 7, 38, -1, 61, 96, -1, 52, 72, -1, -1, 61, -1, 24, 22, -1, -1, -1, -1, -1, -1, -1,
          98, -1, -1, -1, 34, 88, -1, 31, -1, 91, -1, -1, -1, -1, 13, -1, 23, 36, -1, 21, -1, 98, -1, 47, -1,
          15, -1, -1, -1, -1, 69, -1, -1, 54, 48, -1, 74, -1, -1, 10, -1, 66, -1, -1, -1, 96, -1, 20, -1, 73,
          12, 16, 65, 44, 78, 7, -1, 7, -1, 20, 41, 21, 75, 2, 84, 68, 68, 40, 12, -1, -1, -1, -1, 24, -1, 81,
          87, 11, 92, 2, 42, 62, 85, -1, -1, 76, -1, -1, -1, -1),
        (10, 158)
      ) must beTrue
      WMMatchingTest.check(
        20,
        Array(27, 33, 27, 6, 74, 2, 16, 52, 84, 5, 83, 72, 63, 17, 18, 33, 75, 62, 38, 89, 45, 80, 22, 52, 78,
          87, 53, 49, 33, 73, 79, 39, 10, 49, 74, 1, 37, 96, 77, 92, 36, 48, 10, 16, 72, 80, 36, 71, 0, 20, 0,
          24, 37, 36, 94, 64, 39, 85, 28, 48, 8, 1, 89, 58, 89, 45, 61, 79, 53, 38, 7, 0, 96, 23, 98, 67, 68,
          30, 57, 63, 78, 8, 9, 77, 81, 12, 91, 20, 46, 72, 57, 24, 52, 94, 67, 89, 3, 90, 8, 23, 30, 41, 47,
          19, 75, 83, 23, 72, 21, 70, 93, 18, 84, 44, 5, 91, 1, 37, 52, 11, 62, 18, 94, 2, 65, 96, 4, 62, 84,
          6, 44, 55, 67, 81, 96, 21, 41, 77, 78, 60, 32, 12, 41, 60, 68, 96, 63, 26, 10, 28, 49, 74, 78, 85,
          92, 74, 48, 50, 4, 29, 38, 22, 64, 88, 81, 76, 16, 60, 3, 90, 7, 86, 53, 21, 93, 21, 52, 59, 94, 6,
          22, 94, 80, 24, 9, 43, 20, 52, 33, 74),
        (10, 46)
      ) must beTrue

    }
  }
}
