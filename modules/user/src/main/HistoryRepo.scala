package lila.user

import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.db.api._
import lila.db.InColl
import lila.db.Types._
import tube.historyTube

object HistoryRepo {

  def addEntry(userId: String, elo: Int, opponentElo: Option[Int]): Funit =
    $update(
      $select(userId),
      $push("entries", opponentElo.fold(Json.arr(DateTime.now.getSeconds.toInt, elo)) { opElo ⇒
        Json.arr(DateTime.now.getSeconds.toInt, elo, opElo)
      }),
      upsert = true)

  def userElos(userId: String): Fu[Seq[(Int, Int, Option[Int])]] =
    $find.one($select(userId)) map { historyOption ⇒
      ~(for {
        history ← historyOption
        entries ← (history \ "entries").asOpt[JsArray]
        arrays = entries.value.map(_.asOpt[JsArray])
        elems = arrays collect {
          case Some(array) ⇒ (
            array(0).asOpt[Int].getOrElse(0),
            array(1).asOpt[Int].getOrElse(0),
            array(2).asOpt[Int])
        }
      } yield elems)
    }

  // def fixAll = io {
  // import java.lang.Float.parseFloat
  // import java.lang.Integer.parseInt
  //   collection.find() foreach { history ⇒
  //     val initEntries = history.as[MongoDBList]("entries").toList
  //     val entries = (initEntries collect {
  //       case elem: com.mongodb.BasicDBList ⇒ try {
  //         for {
  //           ts ← elem.getAs[Double](0)
  //           elo ← elem.getAs[Double](1)
  //           op = if (elem.size > 2) elem.getAs[Double](2) else None
  //         } yield op.fold(List(ts.toInt, elo.toInt)) { o ⇒
  //           List(ts.toInt, elo.toInt, o.toInt)
  //         }
  //       }
  //       catch {
  //         case (err: Exception) ⇒
  //           for {
  //             ts ← elem.getAs[Int](0)
  //             elo ← elem.getAs[Int](1)
  //             op = if (elem.size > 2) elem.getAs[Int](2) else None
  //           } yield op.fold(List(ts, elo)) { o ⇒ List(ts, elo, o) }
  //       }
  //     }).flatten sortBy (_.head)
  //     val id = history.as[String]("_id")
  //     loginfo("%s: %d -> %d".format(id, initEntries.size, entries.size))
  //     collection.update(
  //       DBObject("_id" -> id),
  //       $set(Seq("entries" -> entries))
  //     )
  //   }
  // }
}
