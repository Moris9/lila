package lila.coordinate

import lila.db.dsl._
import reactivemongo.api.bson._

final class CoordinateApi(scoreColl: Coll) {

  implicit private val scoreBSONHandler = Macros.handler[Score]

  def getScore(userId: String): Fu[Score] =
    scoreColl.byId[Score](userId) map (_ | Score(userId))

  def addScore(userId: String, white: Boolean, hits: Int): Funit =
    scoreColl.update
      .one(
        $id(userId),
        $push(
          $doc(
            "white" -> BSONDocument(
              "$each"  -> (white ?? List(BSONInteger(hits))),
              "$slice" -> -20
            ),
            "black" -> BSONDocument(
              "$each"  -> (!white ?? List(BSONInteger(hits))),
              "$slice" -> -20
            )
          )
        ),
        upsert = true
      )
      .void
}
