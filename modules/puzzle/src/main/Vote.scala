package lila.puzzle

import org.joda.time.DateTime

case class Vote(
    _id: String, // puzzleId/userId
    v: Boolean) {

  def id = _id

  def value = v
}

object Vote {

  def makeId(puzzleId: PuzzleId, userId: String) = s"$puzzleId/$userId"

  implicit val voteBSONHandler = reactivemongo.bson.Macros.handler[Vote]
}
