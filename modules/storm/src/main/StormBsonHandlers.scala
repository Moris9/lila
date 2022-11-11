package lila.storm

import chess.format.{ FEN, Uci }
import reactivemongo.api.bson._

import lila.db.dsl.{ *, given }
import lila.puzzle.Puzzle
import scala.util.Success
import lila.common.LichessDay

object StormBsonHandlers {

  import lila.puzzle.BsonHandlers.PuzzleIdBSONHandler

  implicit val StormPuzzleBSONReader = new BSONDocumentReader[StormPuzzle] {
    def readDocument(r: BSONDocument) = for {
      id      <- r.getAsTry[Puzzle.Id]("_id")
      fen     <- r.getAsTry[FEN]("fen")
      lineStr <- r.getAsTry[String]("line")
      line    <- lineStr.split(' ').toList.flatMap(Uci.Move.apply).toNel.toTry("Empty move list?!")
      rating  <- r.getAsTry[Int]("rating")
    } yield StormPuzzle(id, fen, line, rating)
  }

  implicit lazy val stormDayIdHandler = {
    import StormDay.Id
    val sep = ':'
    tryHandler[Id](
      { case BSONString(v) =>
        v split sep match {
          case Array(userId, dayStr) => Success(Id(userId, LichessDay(Integer.parseInt(dayStr))))
          case _                     => handlerBadValue(s"Invalid storm day id $v")
        }
      },
      id => BSONString(s"${id.userId}$sep${id.day.value}")
    )
  }

  given BSONDocumentHandler[StormDay] = Macros.handler
}
