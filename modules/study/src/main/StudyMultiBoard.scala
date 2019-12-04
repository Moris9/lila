package lila.study

import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import play.api.libs.json._
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import chess.Color
import chess.format.pgn.{ Tag, Tags }
import chess.format.{ FEN, Uci }

import BSONHandlers._
import JsonView._
import lila.common.config.MaxPerPage
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.db.dsl._
import lila.db.paginator.{ Adapter, MapReduceAdapter }
import lila.game.BSONHandlers.FENBSONHandler

final class StudyMultiBoard(
    runCommand: lila.db.RunCommand,
    chapterRepo: ChapterRepo
) {
  private val maxPerPage = MaxPerPage(9)

  import StudyMultiBoard._
  import handlers._

  def json(studyId: Study.Id, page: Int, playing: Boolean): Fu[JsObject] = {
    if (page == 1 && !playing) firstPageCache.get(studyId)
    else fetch(studyId, page, playing)
  } map { PaginatorJson(_) }

  private val firstPageCache: AsyncLoadingCache[Study.Id, Paginator[ChapterPreview]] = Scaffeine()
    .expireAfterWrite(4 seconds)
    .buildAsyncFuture[Study.Id, Paginator[ChapterPreview]] { fetch(_, 1, false) }

  private def fetch(studyId: Study.Id, page: Int, playing: Boolean): Fu[Paginator[ChapterPreview]] = {

    val selector = $doc("studyId" -> studyId) ++ playing.??(playingSelector)

    /* If players are found in the tags,
     * return the last mainline node.
     * Else, return the root node without its children.
     */
    Paginator(
      adapter = new MapReduceAdapter[ChapterPreview](
        collection = chapterRepo.coll,
        selector = selector,
        sort = $sort asc "order",
        runCommand = runCommand,
        command = $doc(
          "map" -> """var node = this.root, child, tagPrefixes = ['White','Black','Result'], result = {name:this.name,orientation:this.setup.orientation,tags:this.tags.filter(t => tagPrefixes.find(p => t.startsWith(p)))};
if (result.tags.length > 1) { while(child = node.n[0]) { node = child }; }
result.fen = node.f;
result.uci = node.u;
emit(this._id, result)""",
          "reduce" -> """function() {}""",
          "jsMode" -> true
        )
      )(previewBSONReader),
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }

  private val playingSelector = $doc("tags" -> "Result:*")

  private object handlers {

    implicit val previewBSONReader = new BSONDocumentReader[ChapterPreview] {
      def readDocument(result: BSONDocument) = for {
        value <- result.getAsTry[List[Bdoc]]("value")
        doc <- value.headOption toTry "No mapReduce value?!"
        tags = doc.getAsOpt[Tags]("tags")
      } yield ChapterPreview(
        id = result.getAsOpt[Chapter.Id]("_id") err "Preview missing id",
        name = doc.getAsOpt[Chapter.Name]("name") err "Preview missing name",
        players = tags flatMap ChapterPreview.players,
        orientation = doc.getAsOpt[Color]("orientation") getOrElse Color.White,
        fen = doc.getAsOpt[FEN]("fen") err "Preview missing FEN",
        lastMove = doc.getAsOpt[Uci]("uci"),
        playing = tags.flatMap(_(_.Result)) has "*"
      )
    }

    implicit val previewPlayerWriter: Writes[ChapterPreview.Player] = Writes[ChapterPreview.Player] { p =>
      Json.obj("name" -> p.name)
        .add("title" -> p.title)
        .add("rating" -> p.rating)
    }

    implicit val previewPlayersWriter: Writes[ChapterPreview.Players] = Writes[ChapterPreview.Players] { players =>
      Json.obj("white" -> players.white, "black" -> players.black)
    }

    implicit val previewWriter: Writes[ChapterPreview] = Json.writes[ChapterPreview]
  }
}

object StudyMultiBoard {

  case class ChapterPreview(
      id: Chapter.Id,
      name: Chapter.Name,
      players: Option[ChapterPreview.Players],
      orientation: Color,
      fen: FEN,
      lastMove: Option[Uci],
      playing: Boolean
  )

  object ChapterPreview {

    case class Player(name: String, title: Option[String], rating: Option[Int])

    type Players = Color.Map[Player]

    def players(tags: Tags): Option[Players] = for {
      wName <- tags(_.White)
      bName <- tags(_.Black)
    } yield Color.Map(
      white = Player(wName, tags(_.WhiteTitle), tags(_.WhiteElo) flatMap (_.toIntOption)),
      black = Player(bName, tags(_.BlackTitle), tags(_.BlackElo) flatMap (_.toIntOption))
    )
  }
}
