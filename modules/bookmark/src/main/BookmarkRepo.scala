package lila.bookmark

import lila.db.Implicits._
import lila.db.api._
import tube.bookmarkTube

import play.api.libs.json._

import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import org.joda.time.DateTime
import scala.concurrent.Future

case class Bookmark(game: lila.game.Game, user: lila.user.User)

// db.bookmark.ensureIndex({g:1})
// db.bookmark.ensureIndex({u:1})
// db.bookmark.ensureIndex({d: -1})
private[bookmark] object BookmarkRepo {

  def toggle(gameId: String, userId: String): Fu[Boolean] =
    $count exists selectId(gameId, userId) flatMap { e ⇒
      e.fold(
        remove(gameId, userId),
        add(gameId, userId, DateTime.now)
      ) inject !e
    }

  def userIdsByGameId(gameId: String): Fu[List[String]] =
    $primitive(Json.obj("g" -> gameId), "u")(_.asOpt[String])

  def gameIdsByUserId(userId: String): Fu[List[String]] =
    $primitive(userIdQuery(userId), "g")(_.asOpt[String])

  def removeByGameId(gameId: String): Funit =
    $remove(Json.obj("g" -> gameId))

  def removeByGameIds(gameIds: List[String]): Funit =
    $remove(Json.obj("g" -> $in(gameIds)))

  private def add(gameId: String, userId: String, date: DateTime): Funit =
    $insert(Json.obj(
      "_id" -> makeId(gameId, userId),
      "g" -> gameId,
      "u" -> userId,
      "d" -> date))

  def userIdQuery(userId: String) = Json.obj("u" -> userId)
  def makeId(gameId: String, userId: String) = gameId + userId
  def selectId(gameId: String, userId: String) = $select(makeId(gameId, userId))

  def remove(gameId: String, userId: String): Funit = $remove(selectId(gameId, userId))
  def remove(selector: JsObject): Funit = $remove(selector)
}
