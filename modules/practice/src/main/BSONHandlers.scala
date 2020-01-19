package lila.practice

import lila.db.dsl._
import lila.study.Chapter
import reactivemongo.api.bson.{ BSONHandler, Macros }

object BSONHandlers {

  import PracticeProgress.{ ChapterNbMoves, NbMoves }

  implicit private val nbMovesHandler: BSONHandler[NbMoves] =
    isoHandler(PracticeProgress.nbMovesIso)
  implicit private val chapterNbMovesHandler: BSONHandler[ChapterNbMoves] =
    typedMapHandler[Chapter.Id, NbMoves](Chapter.idIso)

  implicit val practiceProgressIdHandler =
    stringAnyValHandler[PracticeProgress.Id](_.value, PracticeProgress.Id.apply)
  implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
