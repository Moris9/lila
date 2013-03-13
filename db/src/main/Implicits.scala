package lila.db

import play.api.libs.json._, Json.JsValueWrapper
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.bson._
import reactivemongo.api._
import reactivemongo.api.SortOrder
import play.modules.reactivemongo.Implicits._

object Implicits extends Implicits
trait Implicits {

  type LilaDB = reactivemongo.api.DB

  type ReactiveColl = reactivemongo.api.collections.default.BSONCollection

  type WithStringId = { def id: String }

  type QueryBuilder = GenericQueryBuilder[BSONDocument, BSONDocumentReader, BSONDocumentWriter]

  // hack, this should be in reactivemongo
  implicit def richerQueryBuilder(b: QueryBuilder) = new {

    def sort(sorters: (String, SortOrder)*): QueryBuilder =
      if (sorters.size == 0) b
      else b sort {
        BSONDocument(
          (for (sorter ← sorters) yield sorter._1 -> BSONInteger(
            sorter._2 match {
              case SortOrder.Ascending  ⇒ 1
              case SortOrder.Descending ⇒ -1
            })).toStream)
      }

    // def sortJs(sorter: JsObject): QueryBuilder = b sort JsObjectWriter.write(sorter)

    def limit(nb: Int): QueryBuilder = b.options(b.options batchSize nb)

    def skip(nb: Int): QueryBuilder = b.options(b.options skip nb)
  }
}
