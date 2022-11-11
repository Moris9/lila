package lila.practice

import reactivemongo.api.bson.{ BSONHandler, Macros }

import lila.db.dsl.{ *, given }
import lila.study.Chapter

object BSONHandlers {

  import PracticeProgress.{ ChapterNbMoves, NbMoves }

  implicit private val nbMovesHandler: BSONHandler[NbMoves] =
    isoHandler(PracticeProgress.nbMovesIso)
  implicit private val chapterNbMovesHandler: BSONHandler[ChapterNbMoves] =
    typedMapHandler[Chapter.Id, NbMoves](Chapter.idIso)

  implicit val practiceProgressIdHandler =
    stringAnyValHandler[PracticeProgress.Id](_.value, PracticeProgress.Id.apply)
  given BSONDocumentHandler[PracticeProgress] = Macros.handler
}
