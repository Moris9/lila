package lila.forum

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.User

case class Post(
    id: String,
    topicId: String,
    categId: String,
    author: Option[String],
    userId: Option[String],
    ip: Option[String],
    text: String,
    number: Int,
    troll: Boolean,
    createdAt: DateTime) {

  def showAuthor = (author map (_.trim) filter ("" !=)) | User.anonymous

  def showUserIdOrAuthor = userId | showAuthor

  def isTeam = categId startsWith teamSlug("")

  def isStaff = categId == "staff"
}

object Post {

  val idSize = 8

  def make(
    topicId: String,
    categId: String,
    author: Option[String],
    userId: Option[String],
    ip: Option[String],
    text: String,
    number: Int,
    troll: Boolean): Post = Post(
    id = Random nextString idSize,
    topicId = topicId,
    author = author,
    userId = userId,
    ip = ip,
    text = text,
    number = number,
    troll = troll,
    createdAt = DateTime.now,
    categId = categId)

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[forum] lazy val tube = Tube(
    (__.json update readDate('createdAt)) andThen Json.reads[Post],
    Json.writes[Post] andThen (__.json update writeDate('createdAt))
  )
}
