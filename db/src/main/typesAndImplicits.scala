package lila.db

import play.api.libs.json._, Json.JsValueWrapper
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.bson._
import reactivemongo.api._

object Types extends Types
object Implicits extends Implicits

trait Types {

  type LilaDB = reactivemongo.api.DB

  type ReactiveColl = reactivemongo.api.collections.default.BSONCollection

  type QueryBuilder = GenericQueryBuilder[BSONDocument, BSONDocumentReader, BSONDocumentWriter]

  type Identified[ID] = { def id: ID }

  type Sort = Seq[(String, SortOrder)]
}

trait Implicits extends Types {

  implicit def docId[ID](doc: Identified[ID]): ID = doc.id

  // hack, this should be in reactivemongo
  implicit final class LilaPimpedQueryBuilder(b: QueryBuilder) {

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

    def limit(nb: Int): QueryBuilder = b.options(b.options batchSize nb)

    def skip(nb: Int): QueryBuilder = b.options(b.options skip nb)
  }
}
