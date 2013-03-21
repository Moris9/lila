package lila.i18n

import lila.db.{ Repo, DbApi }
import lila.db.Implicits._

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._

import play.modules.reactivemongo.Implicits._
import play.modules.reactivemongo.MongoJSONHelpers._

private[i18n] final class TranslationRepo(coll: ReactiveColl)
    extends Repo[Translation, Int](coll, Translations.json) {

  val nextId: Fu[Int] = primitive.one(
      select.all, 
      "_id",
      _ sort sort.descId
    )(_.asOpt[Int]) map (opt => ~opt + 1)

  def findFrom(id: Int): Fu[List[Translation]] = 
    find(query(Json.obj("_id" -> $lte(id))) sort sort.ascId)
}
