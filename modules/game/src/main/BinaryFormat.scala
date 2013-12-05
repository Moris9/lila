package lila.game

import scala.util.{ Try, Success, Failure }

import chess._

import lila.db.ByteArray

object BinaryFormat {

  object moveTime {

    private type MT = Int // tenths of seconds
    private val size = 16
    private val encodeList: List[(MT, Int)] = List(1, 5, 10, 15, 20, 30, 40, 50, 60, 80, 100, 150, 200, 300, 400, 600).zipWithIndex
    private val encodeMap: Map[MT, Int] = encodeList.toMap
    private val decodeList: List[(Int, MT)] = encodeList.map(x ⇒ x._2 -> x._1)
    private val decodeMap: Map[Int, MT] = decodeList.toMap

    private def findClose(v: MT, in: List[(MT, Int)]): Option[Int] = in match {
      case (a, b) :: (c, d) :: rest ⇒
        if (math.abs(a - v) <= math.abs(c - v)) Some(b)
        else findClose(v, (c, d) :: rest)
      case (a, b) :: rest ⇒ Some(b)
      case _              ⇒ None
    }

    def write(mts: Vector[MT]): ByteArray = ByteArray {
      def enc(mt: MT) = encodeMap get mt orElse findClose(mt, encodeList) getOrElse (size - 1)
      (mts grouped 2 map {
        case Vector(a, b) ⇒ (enc(a) << 4) + enc(b)
        case Vector(a)    ⇒ enc(a) << 4
      }).map(_.toByte).toArray
    }

    def read(ba: ByteArray): Vector[MT] = {
      def dec(x: Int) = decodeMap get x getOrElse decodeMap(size - 1)
      ba.value map toInt flatMap { k ⇒
        Array(dec(k >> 4), dec(k & 15))
      }
    }.toVector
  }

  object clock {

    def write(clock: Clock): ByteArray = ByteArray {
      def time(t: Float) = writeSignedInt24((t * 100).toInt)
      def timer(seconds: Double) = writeLong40((seconds * 100).toLong)
      Array(writeInt8(clock.limit / 60), writeInt8(clock.increment)) ++
        time(clock.whiteTime) ++
        time(clock.blackTime) ++
        timer(clock.timerOption getOrElse 0d) map (_.toByte)
    }

    def read(ba: ByteArray): Color ⇒ Clock = color ⇒ ba.value map toInt match {
      case Array(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13) ⇒
        readLong40(b9, b10, b11, b12, b13) match {
          case 0 ⇒ PausedClock(
            color = color,
            limit = b1 * 60,
            increment = b2,
            whiteTime = readSignedInt24(b3, b4, b5).toFloat / 100,
            blackTime = readSignedInt24(b6, b7, b8).toFloat / 100)
          case timer ⇒ RunningClock(
            color = color,
            limit = b1 * 60,
            increment = b2,
            whiteTime = readSignedInt24(b3, b4, b5).toFloat / 100,
            blackTime = readSignedInt24(b6, b7, b8).toFloat / 100,
            timer = timer.toDouble / 100)
        }
      case x => sys error s"BinaryFormat.clock.read invalid bytes: ${ba.showBytes}"
    }
  }

  object castleLastMoveTime {

    def write(clmt: CastleLastMoveTime): ByteArray = {

      val castleInt = clmt.castles.toList.zipWithIndex.foldLeft(0) {
        case (acc, (false, _)) ⇒ acc
        case (acc, (true, p))  ⇒ acc + (1 << (3 - p))
      }

      def posInt(pos: Pos): Int = ((pos.x - 1) << 3) + pos.y - 1
      val lastMoveInt = clmt.lastMove.fold(0) {
        case (f, t) ⇒ (posInt(f) << 6) + posInt(t)
      }
      val time = clmt.lastMoveTime getOrElse 0

      val ints = Array(
        (castleInt << 4) + (lastMoveInt >> 8),
        (lastMoveInt & 255)
      ) ++ writeInt24(time)

      ByteArray(ints.map(_.toByte))
    }

    def read(ba: ByteArray): CastleLastMoveTime = {
      def posAt(x: Int, y: Int) = Pos.posAt(x + 1, y + 1)
      ba.value map toInt match {
        case Array(b1, b2, b3, b4, b5) ⇒ CastleLastMoveTime(
          castles = Castles(b1 > 127, (b1 & 64) != 0, (b1 & 32) != 0, (b1 & 16) != 0),
          lastMove = for {
            from ← posAt((b1 & 15) >> 1, ((b1 & 1) << 2) + (b2 >> 6))
            to ← posAt((b2 & 63) >> 3, b2 & 7)
            if from != to
          } yield from -> to,
          lastMoveTime = readInt24(b3, b4, b5).some filter (0 !=))
        case x => sys error s"BinaryFormat.clmt.read invalid bytes: ${ba.showBytes}"
      }
    }
  }

  object piece {

    def write(all: AllPieces): ByteArray = {
      val (alives, deads) = all
      def posInt(pos: Pos): Int = (alives get pos).fold(0)(pieceInt)
      def pieceInt(piece: Piece): Int =
        piece.color.fold(0, 8) + roleToInt(piece.role)
      val aliveBytes: Iterator[Int] = Pos.all grouped 2 map {
        case List(p1, p2) ⇒ (posInt(p1) << 4) + posInt(p2)
      }
      val deadBytes: Iterator[Int] = deads grouped 2 map {
        case List(d1, d2) ⇒ (pieceInt(d1) << 4) + pieceInt(d2)
        case List(d1)     ⇒ pieceInt(d1) << 4
      }
      val bytes = aliveBytes.toArray ++ deadBytes
      ByteArray(bytes.map(_.toByte))
    }

    def read(ba: ByteArray): AllPieces = {
      def splitInts(int: Int) = Array(int >> 4, int & 0x0F)
      def intPiece(int: Int): Option[Piece] =
        intToRole(int & 7) map { role ⇒ Piece(Color((int & 8) == 0), role) }
      val (aliveInts, deadInts) = ba.value map toInt flatMap splitInts splitAt 64
      val alivePieces = (Pos.all zip aliveInts map {
        case (pos, int) ⇒ intPiece(int) map (pos -> _)
      }).flatten.toMap
      alivePieces -> (deadInts map intPiece).toList.flatten
    }

    // cache standard start position
    val standard = write(Board.init(Variant.Standard).pieces -> Nil)

    private def intToRole(int: Int): Option[Role] = int match {
      case 6 ⇒ Some(Pawn)
      case 1 ⇒ Some(King)
      case 2 ⇒ Some(Queen)
      case 3 ⇒ Some(Rook)
      case 4 ⇒ Some(Knight)
      case 5 ⇒ Some(Bishop)
      case _ ⇒ None
    }
    private def roleToInt(role: Role): Int = role match {
      case Pawn   ⇒ 6
      case King   ⇒ 1
      case Queen  ⇒ 2
      case Rook   ⇒ 3
      case Knight ⇒ 4
      case Bishop ⇒ 5
    }
  }

  @inline private def toInt(b: Byte): Int = b & 0xff

  def writeInt8(int: Int) = math.min(255, int)

  private val int24Max = math.pow(2, 24).toInt
  def writeInt24(int: Int) = {
    val i = math.min(int24Max, int)
    Array(i >> 16, (i >> 8) & 255, i & 255)
  }
  def readInt24(b1: Int, b2: Int, b3: Int) = (b1 << 16) + (b2 << 8) + b3

  private val int23Max = math.pow(2, 23).toInt
  def writeSignedInt24(int: Int) = {
    val i = math.abs(math.min(int23Max, int))
    val j = if (int < 0) i + int23Max else i
    Array(j >> 16, (j >> 8) & 255, j & 255)
  }
  def readSignedInt24(b1: Int, b2: Int, b3: Int) = {
    val i = (b1 << 16) + (b2 << 8) + b3
    if (i > int23Max) int23Max - i else i
  }

  private val long40Max = math.pow(2, 40).toLong
  def writeLong40(long: Long) = {
    val i = math.min(long40Max, long)
    Array(i >> 32, (i >> 24) & 255, (i >> 16) & 255, (i >> 8) & 255, i & 255) map (_.toInt)
  }
  def readLong40(b1: Int, b2: Int, b3: Int, b4: Int, b5: Int) =
    (b1.toLong << 32) + (b2 << 24) + (b3 << 16) + (b4 << 8) + b5
}
