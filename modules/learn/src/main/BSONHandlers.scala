package lila.learn

import reactivemongo.bson._

import lila.common.Iso
import lila.db.BSON
import lila.db.dsl._

object BSONHandlers {

  import LearnProgress.Id
  import StageProgress.Score

  private implicit val ScoreHandler = intAnyValHandler[Score](_.value, Score.apply)
  private implicit val ScoresHandler = bsonArrayToVectorHandler[Score]
  private implicit val StageProgressHandler =
    isoHandler[StageProgress, Vector[Score], BSONArray](
      (s: StageProgress) => s.scores, StageProgress.apply _)(ScoresHandler)

  private implicit val LearnProgressStagesHandler = BSON.MapValue.MapHandler[String, StageProgress]
  implicit val LearnProgressIdHandler = stringAnyValHandler[Id](_.value, Id.apply)
  implicit val LearnProgressHandler = Macros.handler[LearnProgress]
}
