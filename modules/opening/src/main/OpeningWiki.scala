package lila.opening

import chess.opening.FullOpening
import com.softwaremill.tagging._
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import reactivemongo.api.bson.Macros
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Markdown
import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

case class OpeningWiki(markup: Option[String], revisions: List[OpeningWiki.Revision])

final class OpeningWikiApi(coll: Coll @@ WikiColl, cacheApi: CacheApi)(implicit ec: ExecutionContext) {

  import OpeningWiki.Revision

  implicit val revisionHandler = Macros.handler[Revision]
  implicit val wikiHandler     = Macros.handler[OpeningWiki]

  def apply(op: FullOpening, withRevisions: Boolean): Fu[OpeningWiki] = for {
    wiki <- cache.get(op.key)
    revisions <- withRevisions ?? {
      coll.primitiveOne[List[Revision]]($id(op.key), "revisions")
    }
  } yield wiki.copy(revisions = (~revisions) take 25)

  def write(op: FullOpening, text: String, by: User): Funit =
    coll.update
      .one(
        $id(op.key),
        $doc(
          "$push" -> $doc(
            "revisions" -> $doc(
              "$each"     -> List(Revision(Markdown(text), by.id, DateTime.now)),
              "$position" -> 0,
              "$slice"    -> 30
            )
          )
        ),
        upsert = true
      )
      .void >>- cache.put(op.key, compute(op.key))

  private val renderer =
    new lila.common.MarkdownRender(
      autoLink = true,
      list = true,
      header = true,
      table = false,
      strikeThrough = false
    )

  private val cache = cacheApi[Opening.Key, OpeningWiki](1024, "opening.wiki") {
    _.maximumSize(1024).buildAsyncFuture(compute)
  }

  private def compute(key: Opening.Key): Fu[OpeningWiki] =
    coll
      .aggregateOne() { framework =>
        import framework._
        Match($id(key)) ->
          List(Project($doc("lastRev" -> $doc("$first" -> "$revisions"))))
      }
      .map { _.flatMap(_.getAsOpt[Revision]("lastRev")(revisionHandler)) }
      .map { lastRev =>
        OpeningWiki(lastRev.map(_.text) map renderer(s"opening:op.key"), Nil)
      }
}

object OpeningWiki {

  case class Revision(text: Markdown, by: User.ID, at: DateTime)

  val form = Form(single("text" -> nonEmptyText(minLength = 10, maxLength = 10_000)))
}
