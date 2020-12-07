package lila.puzzle

import akka.pattern.ask
import org.joda.time.DateTime
import Puzzle.{ BSONFields => F }
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi._

final private[puzzle] class DailyPuzzle(
    colls: PuzzleColls,
    pathApi: PuzzlePathApi,
    renderer: lila.hub.actors.Renderer,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  private val cache =
    cacheApi.unit[Option[DailyPuzzle.Html]] {
      _.refreshAfterWrite(30 minutes)
        .buildAsyncFuture(_ => find)
    }

  def get: Fu[Option[DailyPuzzle.Html]] = cache.getUnit

  private def find: Fu[Option[DailyPuzzle.Html]] =
    (findCurrent orElse findNew) recover { case e: Exception =>
      logger.error("find daily", e)
      none
    } flatMap { _ ?? makeDaily }

  private def makeDaily(puzzle: Puzzle): Fu[Option[DailyPuzzle.Html]] = {
    import makeTimeout.short
    renderer.actor ? DailyPuzzle.Render(puzzle, puzzle.fenAfterInitialMove, puzzle.line.head.uci) map {
      case html: String => DailyPuzzle.Html(html, puzzle.color, puzzle.id).some
    }
  } recover { case e: Exception =>
    logger.warn("make daily", e)
    none
  }

  private def findCurrent =
    colls.puzzle {
      _.find($doc(F.day $gt DateTime.now.minusMinutes(24 * 60 - 15)))
        .one[Puzzle]
    }

  private def findNew =
    colls
      .path {
        _.aggregateOne() { framework =>
          import framework._
          Match(pathApi.select(PuzzleTheme.any.key, PuzzleTier.Top, 1200 to 2200)) -> List(
            Sample(1),
            Project($doc("ids" -> true))
          )
        }.dmap(~_.flatMap(_.getAsOpt[List[Puzzle.Id]]("ids")))
      } flatMap { ids =>
      colls.puzzle { c =>
        c.find($doc(F.id $in ids, F.day $exists false))
          .sort($doc(F.vote -> -1))
          .one[Puzzle] flatMap {
          case Some(puzzle) =>
            c.update.one(
              $id(puzzle.id),
              $set(F.day -> DateTime.now)
            ) inject puzzle.some
          case None => fuccess(none)
        }
      }
    }
}

object DailyPuzzle {
  type Try = () => Fu[Option[DailyPuzzle.Html]]

  case class Html(html: String, color: chess.Color, id: Puzzle.Id)

  case class Render(puzzle: Puzzle, fen: chess.format.FEN, lastMove: String)
}
