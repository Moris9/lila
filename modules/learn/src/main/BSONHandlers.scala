package lila.learn

import reactivemongo.bson.{ MapReader => _, MapWriter => _, _ }

import lila.common.Iso
import lila.db.BSON
import lila.db.dsl._

object BSONHandlers {

  import StageProgress.Score

  private implicit val ScoreHandler = intAnyValHandler[Score](_.value, Score.apply)
  private implicit val ScoresHandler = bsonArrayToVectorHandler[Score]
  private implicit val StageProgressHandler =
    isoHandler[StageProgress, Vector[Score], BSONArray](
      (s: StageProgress) => s.scores, StageProgress.apply _
    )(ScoresHandler)

  implicit val LearnProgressIdHandler = stringAnyValHandler[LearnProgress.Id](_.value, LearnProgress.Id.apply)
  implicit val LearnProgressHandler = Macros.handler[LearnProgress]
}
