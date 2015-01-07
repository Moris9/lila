package lila.opening

import chess.Color
import org.joda.time.DateTime

case class Move(
  first: String,
  cp: Int,
  line: List[String])

case class Opening(
    id: Opening.ID,
    fen: String,
    moves: List[Move],
    color: Color,
    date: DateTime,
    attempts: Int,
    score: Double) {

  def goal = qualityMoves.size min 5

  lazy val qualityMoves: List[QualityMove] = {
    val bestCp = moves.foldLeft(Int.MaxValue) {
      case (cp, move) => if (move.cp < cp) move.cp else cp
    }
    moves.map { move =>
      QualityMove(move, Quality(move.cp - bestCp))
    }
  }
}

sealed abstract class Quality(val threshold: Int) {
  val name = toString.toLowerCase
}
object Quality {
  case object Good extends Quality(30)
  case object Dubious extends Quality(80)
  case object Bad extends Quality(Int.MaxValue)

  def apply(cp: Int) =
    if (cp < Good.threshold) Good
    else if (cp < Dubious.threshold) Dubious
    else Bad
}

case class QualityMove(
  move: Move,
  quality: Quality)

object Opening {

  type ID = Int

  val defaultScore = 50

  def make(
    fen: String,
    color: Color,
    moves: List[Move])(id: ID) = new Opening(
    id = id,
    fen = fen,
    moves = moves,
    color = color,
    date = DateTime.now,
    attempts = 0,
    score = 50)

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler

  implicit val moveBSONHandler = new BSON[Move] {

    def reads(r: BSON.Reader): Move = Move(
      first = r str "first",
      cp = r int "cp",
      line = chess.format.pgn.Binary.readMoves(r.bytes("line").value.toList).get)

    def writes(w: BSON.Writer, o: Move) = BSONDocument(
      "first" -> o.first,
      "cp" -> o.cp,
      "line" -> lila.db.ByteArray {
        chess.format.pgn.Binary.writeMoves(o.line).get.toArray
      })
  }

  object BSONFields {
    val id = "_id"
    val fen = "fen"
    val moves = "moves"
    val white = "white"
    val date = "date"
    val attempts = "attempts"
    val score = "score"
  }

  implicit val openingBSONHandler = new BSON[Opening] {

    import BSONFields._

    def reads(r: BSON.Reader): Opening = Opening(
      id = r int id,
      fen = r str fen,
      moves = r.get[List[Move]](moves),
      color = Color(r bool white),
      date = r date date,
      attempts = r int attempts,
      score = r double score)

    def writes(w: BSON.Writer, o: Opening) = BSONDocument(
      id -> o.id,
      fen -> o.fen,
      moves -> o.moves,
      white -> o.color.white,
      date -> o.date,
      attempts -> o.attempts,
      score -> o.score)
  }
}
