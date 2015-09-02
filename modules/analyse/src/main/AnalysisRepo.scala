package lila.analyse

import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter

import lila.db.api._
import lila.db.Implicits._
import tube.analysisTube

object AnalysisRepo {

  type ID = String

  def done(id: ID, a: Analysis, serverIp: String) = $update(
    $select(id),
    $set(Json.obj(
      "done" -> true,
      "data" -> Info.encodeList(a.infos),
      "ip" -> serverIp
    ))
  )

  def progress(id: ID, userId: ID, startPly: Int) = $update(
    $select(id),
    $set(
      Json.obj(
        "uid" -> userId,
        "done" -> false,
        "date" -> $date(DateTime.now)
      ) ++ (startPly == 0).fold(Json.obj(), Json.obj("ply" -> startPly))
    ) ++ $unset("old", "data"),
    upsert = true)

  def byId(id: ID): Fu[Option[Analysis]] = $find byId id

  def doneById(id: ID): Fu[Option[Analysis]] =
    $find.one($select(id) ++ Json.obj("done" -> true))

  def notDoneById(id: ID): Fu[Option[Analysis]] =
    $find.one($select(id) ++ Json.obj("done" -> false))

  def doneByIds(ids: Seq[ID]): Fu[Seq[Option[Analysis]]] =
    $find optionsByOrderedIds ids map2 { (a: Option[Analysis]) =>
      a.filter(_.done)
    }

  def doneByIdNotOld(id: ID): Fu[Option[Analysis]] =
    $find.one($select(id) ++ Json.obj("done" -> true, "old" -> $exists(false)))

  def isDone(id: ID): Fu[Boolean] =
    $count.exists($select(id) ++ Json.obj("done" -> true))

  def recent(nb: Int): Fu[List[Analysis]] =
    $find($query(Json.obj("done" -> true)) sort $sort.desc("date"), nb)

  def skipping(skip: Int, nb: Int): Fu[List[Analysis]] =
    $find($query(Json.obj("done" -> true)) skip skip, nb)

  def count = $count($select.all)

  def remove(id: String) = $remove byId id
}
