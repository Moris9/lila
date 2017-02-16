package lila.game

import scala.concurrent.duration._

import lila.db.dsl._
import lila.db.ByteArray
import lila.user.{ User, UserRepo }

import reactivemongo.bson._

final class PlayTime(gameColl: Coll) {

  private val moveTimeField = Game.BSONFields.moveTimes
  private val tvField = Game.BSONFields.tvAt

  def apply(user: User): Fu[User.PlayTime] = user.playTime match {
    case Some(pt) => fuccess(pt)
    case _ => {
      gameColl
        .find($doc(
          Game.BSONFields.playerUids -> user.id,
          Game.BSONFields.status -> $doc("$gte" -> chess.Status.Mate.id)
        ))
        .projection($doc(
          moveTimeField -> true,
          tvField -> true
        ))
        .cursor[Bdoc]().fold(User.PlayTime(0, 0)) { (pt, doc) =>
          val t = (doc.getAs[ByteArray](moveTimeField) ?? { times =>
            BinaryFormat.moveTime.read(times).fold(0 millis)(_ + _)
          } toSeconds).toInt
          val isTv = doc.get(tvField).isDefined
          User.PlayTime(pt.total + t, pt.tv + isTv.fold(t, 0))
        }
    }.addEffect { UserRepo.setPlayTime(user, _) }
  }
}
