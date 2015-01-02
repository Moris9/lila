package lila.history

import org.joda.time.{ DateTime, Days }
import reactivemongo.bson._

import chess.Speed
import lila.db.api._
import lila.db.Types.Coll
import lila.game.Game
import lila.user.{ User, Perfs }

final class HistoryApi(coll: Coll) {

  import History.BSONReader

  def add(user: User, game: Game, perfs: Perfs): Funit = {
    val isStd = game.variant.standard
    val changes = List(
      isStd.option("standard" -> perfs.standard),
      game.variant.chess960.option("chess960" -> perfs.chess960),
      game.variant.kingOfTheHill.option("kingOfTheHill" -> perfs.kingOfTheHill),
      game.variant.threeCheck.option("threeCheck" -> perfs.threeCheck),
      game.variant.antichess.option("antichess" -> perfs.antichess),
      game.variant.atomicChess.option("atomicChess" -> perfs.atomicChess),
      (isStd && game.speed == Speed.Bullet).option("bullet" -> perfs.bullet),
      (isStd && game.speed == Speed.Blitz).option("blitz" -> perfs.blitz),
      (isStd && game.speed == Speed.Classical).option("classical" -> perfs.classical),
      (isStd && game.speed == Speed.Correspondence).option("correspondence" -> perfs.correspondence)
    ).flatten.map {
        case (k, p) => k -> p.intRating
      }
    val days = daysBetween(user.createdAt, game.updatedAt | game.createdAt)
    coll.update(
      BSONDocument("_id" -> user.id),
      BSONDocument("$set" -> BSONDocument(changes.map {
        case (perf, rating) => s"$perf.$days" -> BSONInteger(rating)
      })),
      upsert = true
    ).void
  }

  def daysBetween(from: DateTime, to: DateTime): Int =
    Days.daysBetween(from.withTimeAtStartOfDay, to.withTimeAtStartOfDay).getDays

  def get(userId: String): Fu[Option[History]] =
    coll.find(BSONDocument("_id" -> userId)).one[History]
}
