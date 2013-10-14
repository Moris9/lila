package lila.analyse

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json.Json

import lila.db.api._
import lila.db.Implicits._
import tube.analysisTube

private[analyse] object AnalysisRepo {

  type ID = String

  def done(id: ID, a: Analysis) = $update(
    $select(id),
    $set(Json.obj(
      "done" -> true,
      "data" -> Info.encodeList(a.infos)
    ))
  )

  def progress(id: ID, userId: ID) = $update(
    $select(id),
    $set(Json.obj(
      "uid" -> userId,
      "done" -> false,
      "date" -> $date(DateTime.now)
    )),
    upsert = true)

  def doneById(id: ID): Fu[Option[Analysis]] =
    $find.one($select(id) ++ Json.obj("done" -> true))

  def isDone(id: ID): Fu[Boolean] =
    $count.exists($select(id) ++ Json.obj("done" -> true))

  def userInProgress(uid: ID): Fu[Option[String]] = $primitive.one(
    Json.obj(
      "uid" -> uid, 
      "done" -> false, 
      "date" -> $gt($date(DateTime.now - 20.minutes))),
    "_id")(_.asOpt[String])

  def getOrRemoveStaled(id: ID): Fu[Option[Analysis]] =
    $find.one($select(id) ++ Json.obj(
      "done" -> false, 
      "date" -> $lt($date(DateTime.now - 20.minutes)))) flatMap {
      _.fold($find byId id) { staled ⇒ $remove byId id inject none }
    }

  def count = $count($select.all)
}
