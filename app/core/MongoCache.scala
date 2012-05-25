package lila
package core

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

final class MongoCache(collection: MongoCollection) {

  private val field = "v"

  def set(key: String, value: Any) {
    collection += DBObject("_id" -> key, field -> value)
  }

  def get(key: String): Option[Any] = for {
    o ← collection.findOne(select(key))
    v ← Option(o get field)
  } yield v

  def getAs[T](key: String)(implicit m: ClassManifest[T]): Option[T] = for {
    v ← get(key)
    typed ← (m.erasure.isAssignableFrom(v.getClass)) option v.asInstanceOf[T]
  } yield typed

  def remove(key: String) {
    collection remove select(key)
  }

  private def select(key: String) = DBObject("_id" -> key)
}
