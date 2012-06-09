package lila
package round

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

class WatcherRoomRepo(collection: MongoCollection)
    extends SalatDAO[WatcherRoom, String](collection) {

  def room(id: String): IO[WatcherRoom] = io {
    findOneByID(id) | WatcherRoom(id, Nil)
  }

  def addMessage(id: String, author: String, text: String): IO[Unit] = io {
    collection.update(
      DBObject("_id" -> id),
      $push("messages" -> WatcherRoom.encode(author, text)),
      upsert = true,
      multi = false
    )
  }
}
