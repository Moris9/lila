package lila.system
package repo

import model._

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

class GameRepo(collection: MongoCollection) extends SalatDAO[DbGame, String](collection) {

  def game(id: String): Option[DbGame] = findOneByID(id)

  def anyGame = findOne(DBObject())
}
