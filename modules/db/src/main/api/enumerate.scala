package lila.db
package api

import play.api.libs.iteratee._
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.Cursor
import reactivemongo.bson._
import scalaz.Monoid

import lila.db.Implicits._

object $enumerate {

  def apply[A: BSONDocumentReader](query: QueryBuilder)(op: A ⇒ Funit): Funit =
    query.cursor[A].enumerate() run {
      Iteratee.foreach((obj: A) ⇒ op(obj))
    }

  def bulk[A: BSONDocumentReader](query: QueryBuilder, size: Int, limit: Int = Int.MaxValue)(op: List[A] ⇒ Funit): Funit =
    query.batch(size).cursor[A].enumerateBulks(limit) run {
      Iteratee.foreach((objs: Iterator[A]) ⇒
        op(objs.toList)
      )
    }

  def fold[A: BSONDocumentReader, B](query: QueryBuilder)(zero: B)(f: (B, A) ⇒ B): Fu[B] =
    query.cursor[A].enumerate() |>>> Iteratee.fold(zero)(f)

  def foldMonoid[A: BSONDocumentReader, B: Monoid](query: QueryBuilder)(f: (B, A) ⇒ B): Fu[B] =
    fold(query)(Monoid[B].zero)(f)
}
