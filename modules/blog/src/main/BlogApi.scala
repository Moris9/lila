package lila.blog

import io.prismic._
import lila.memo.AsyncCache
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

final class BlogApi(prismicUrl: String, collection: String) {

  def recent(api: Api, ref: Option[String], nb: Int): Fu[Option[Response]] =
    api.forms(collection).ref(ref | api.master.ref)
      .orderings(s"[my.$collection.date desc]")
      .pageSize(nb).page(1).submit().fold(_ => none, some _)

  def one(api: Api, ref: Option[String], id: String) =
    api.forms(collection)
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(ref | api.master.ref).submit() map (_.results.headOption)

  // -- Build a Prismic context
  def context(ref: Option[String])(implicit linkResolver: (Api, Option[String]) => DocumentLinkResolver) =
    prismicApi map { api =>
      play.api.Logger("prismic").debug(s"use master ref: ${api.master.ref}")
      BlogApi.Context(
        api,
        ref.map(_.trim).filterNot(_.isEmpty).getOrElse(api.master.ref),
        linkResolver(api, ref))
    }

  private val cache = BuiltInCache(200)
  private val logger = (level: Symbol, message: String) => level match {
    case 'DEBUG => play.api.Logger("prismic") debug message
    case 'ERROR => play.api.Logger("prismic") error message
    case _      => play.api.Logger("prismic") info message
  }

  private val fetchPrismicApi = AsyncCache.single[Api](
    f = {
      play.api.Logger("prismic").debug(s"fetching API")
      Api.get(prismicUrl, cache = cache, logger = logger) addEffect { api =>
        play.api.Logger("prismic").debug(s"fetched master ref: ${api.master.ref}")
      }
    },
    timeToLive = 5 seconds)

  def prismicApi = fetchPrismicApi(true)
}

object BlogApi {

  def extract(body: Fragment.StructuredText): String =
    body.blocks
      .takeWhile(_.isInstanceOf[Fragment.StructuredText.Block.Paragraph])
      .take(2).map {
        case Fragment.StructuredText.Block.Paragraph(text, _, _) => s"<p>$text</p>"
        case _ => ""
      }.mkString

  case class Context(api: Api, ref: String, linkResolver: DocumentLinkResolver) {
    def maybeRef = Option(ref).filterNot(_ == api.master.ref)
  }
}
