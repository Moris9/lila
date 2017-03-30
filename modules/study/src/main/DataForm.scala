package lila.study

import play.api.data._
import play.api.data.Forms._

object DataForm {

  lazy val form = Form(mapping(
    "gameId" -> optional(nonEmptyText),
    "orientation" -> optional(nonEmptyText),
    "fen" -> optional(nonEmptyText),
    "pgn" -> optional(nonEmptyText),
    "variant" -> optional(nonEmptyText),
    "as" -> optional(nonEmptyText)
  )(Data.apply)(Data.unapply))

  case class Data(
      gameId: Option[String] = None,
      orientationStr: Option[String] = None,
      fenStr: Option[String] = None,
      pgnStr: Option[String] = None,
      variantStr: Option[String] = None,
      asStr: Option[String] = None
  ) {

    def orientation = orientationStr.flatMap(chess.Color.apply) | chess.White

    def as: As = asStr match {
      case None | Some("study") => AsNewStudy
      case Some(studyId) => AsChapterOf(Study.Id(studyId))
    }

    def toChapterData = ChapterMaker.Data(
      name = Chapter.Name(""),
      game = gameId,
      variant = variantStr,
      fen = fenStr,
      pgn = pgnStr,
      orientation = orientation.name,
      mode = ChapterMaker.Mode.Normal.key,
      initial = false
    )
  }

  sealed trait As
  case object AsNewStudy extends As
  case class AsChapterOf(studyId: Study.Id) extends As
}
