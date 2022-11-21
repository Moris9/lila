package lila.round

import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.db.dsl.{ *, given }
import lila.game.Game

final class NoteApi(coll: Coll)(using ec: scala.concurrent.ExecutionContext):

  def collName  = coll.name
  val noteField = "t"

  def get(gameId: GameId, userId: String): Fu[String] =
    coll.primitiveOne[String]($id(makeId(gameId, userId)), noteField) dmap (~_)

  def set(gameId: GameId, userId: String, text: String) = {
    if (text.isEmpty) coll.delete.one($id(makeId(gameId, userId)))
    else
      coll.update.one(
        $id(makeId(gameId, userId)),
        $set(noteField -> text),
        upsert = true
      )
  }.void

  def byGameIds(gameIds: Seq[GameId], userId: String): Fu[Map[GameId, String]] =
    coll.byIds(gameIds.map(makeId(_, userId)), ReadPreference.secondaryPreferred) map { docs =>
      (for {
        doc    <- docs
        noteId <- doc.string("_id")
        note   <- doc.string(noteField)
      } yield (Game takeGameId noteId, note)).toMap
    }

  private def makeId(gameId: String, userId: String) = s"$gameId$userId"
