package lila.round

import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.db.dsl.{ *, given }
import lila.game.Game

final class NoteApi(coll: Coll)(using ec: scala.concurrent.ExecutionContext):

  def collName  = coll.name
  val noteField = "t"

  def get(gameId: Game.Id, userId: String): Fu[String] =
    coll.primitiveOne[String]($id(makeId(gameId, userId)), noteField) dmap (~_)

  def set(gameId: Game.Id, userId: String, text: String) = {
    if (text.isEmpty) coll.delete.one($id(makeId(gameId, userId)))
    else
      coll.update.one(
        $id(makeId(gameId, userId)),
        $set(noteField -> text),
        upsert = true
      )
  }.void

  def byGameIds(gameIds: Seq[Game.Id], userId: String): Fu[Map[Game.Id, String]] =
    coll.byIds(gameIds.map(makeId(_, userId)), ReadPreference.secondaryPreferred) map { docs =>
      (for {
        doc    <- docs
        noteId <- doc.getAsOpt[String]("_id")
        note   <- doc.getAsOpt[String](noteField)
      } yield (noteId take Game.gameIdSize, note)).toMap
    }

  private def makeId(gameId: String, userId: String) = s"$gameId$userId"
