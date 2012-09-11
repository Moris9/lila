package lila
package message

import user.User

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import ornicar.scalalib.Random

case class Thread(
    @Key("_id") id: String,
    name: String,
    createdAt: DateTime,
    updatedAt: DateTime,
    posts: List[Post],
    creatorId: String,
    invitedId: String,
    visibleByUserIds: List[String]) {

  def +(post: Post) = copy(
    posts = posts :+ post,
    updatedAt = post.createdAt)

  def isCreator(user: User) = creatorId == user.id

  def isReadBy(user: User) = nbUnreadBy(user) == 0

  def isUnReadBy(user: User) = !isReadBy(user) 

  def nbUnreadBy(user: User): Int = isCreator(user).fold(
    posts count { post ⇒ post.isByInvited && post.isUnRead },
    posts count { post ⇒ post.isByCreator && post.isUnRead })

  def nbUnread: Int = posts count (_.isUnRead)

  def userIds = List(creatorId, invitedId)

  def hasUser(user: User) = userIds contains user.id

  def otherUserId(user: User) = isCreator(user).fold(invitedId, creatorId)

  def senderOf(post: Post) = post.isByCreator.fold(creatorId, invitedId)

  def receiverOf(post: Post) = post.isByCreator.fold(invitedId, creatorId)

  def nonEmptyName = (name.trim.some filter ("" !=)) | "No subject"
}

object Thread {

  val idSize = 8

  def apply(
    name: String,
    text: String,
    creator: User,
    invited: User): Thread = Thread(
    id = Random nextString idSize,
    name = name,
    createdAt = DateTime.now,
    updatedAt = DateTime.now,
    posts = List(Post(
      text = text,
      isByCreator = true
    )),
    creatorId = creator.id,
    invitedId = invited.id,
    visibleByUserIds = List(creator.id, invited.id))
}
