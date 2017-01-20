package lila.learn

import reactivemongo.bson._

import lila.common.Iso
import lila.db.BSON
import lila.db.dsl._

object BSONHandlers {

  import StageProgress.Score

  private implicit val ScoreHandler = intAnyValHandler[Score](_.value, Score.apply)
  private implicit val ScoresHandler = bsonArrayToVectorHandler[Score]
  private implicit val StageProgressHandler =
    isoHandler[StageProgress, Vector[Score], BSONArray](
      (s: StageProgress) => s.scores, StageProgress.apply _)(ScoresHandler)

  implicit val LearnProgressIdHandler = new BSONHandler[BSONString, LearnProgress.Id] {
    def read(bs: BSONString): LearnProgress.Id =
      if (bs.value startsWith "anon:") LearnProgress.AnonId(bs.value drop 5)
      else LearnProgress.UserId(bs.value)
    def write(x: LearnProgress.Id) = BSONString(x.str)
  }
  private implicit val LearnProgressStagesHandler = BSON.MapValue.MapHandler[StageProgress]
  implicit val LearnProgressHandler = Macros.handler[LearnProgress]
}
