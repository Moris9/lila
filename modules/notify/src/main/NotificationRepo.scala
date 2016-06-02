package lila.notify

import lila.db.dsl._
import org.joda.time.DateTime

private final class NotificationRepo(val coll: Coll) {

  import BSONHandlers._

  def insert(notification: Notification) = {
    coll.insert(notification).void
  }

  def markAllRead(notifies: Notification.Notifies): Funit = {
    coll.update(unreadOnlyQuery(notifies), $set("read" -> true), multi = true).void
  }

  def unreadNotificationsCount(userId: Notification.Notifies): Fu[Int] = {
    coll.count(unreadOnlyQuery(userId).some)
  }

  private val hasOld = $doc(
    "read" -> false,
    "createdAt" -> $doc("$gt" -> DateTime.now.minusDays(3)))
  private val hasUnread = $doc( // recent, read
    "createdAt" -> $doc("$gt" -> DateTime.now.minusMinutes(10)))
  private val hasOldOrUnread =
    $doc("$or" -> List(hasOld, hasUnread))

  def hasRecentStudyInvitation(userId: Notification.Notifies, studyId: InvitedToStudy.StudyId): Fu[Boolean] =
    coll.exists($doc(
      "notifies" -> userId,
      "content.type" -> "invitedStudy",
      "content.studyId" -> studyId
    ) ++ hasOldOrUnread)

  def hasRecentNotificationsInThread(userId: Notification.Notifies, topicId: MentionedInThread.TopicId): Fu[Boolean] =
    coll.exists($doc(
      "notifies" -> userId,
      "content.type" -> "mention",
      "content.topicId" -> topicId
    ) ++ hasOldOrUnread)

  def hasRecentPrivateMessageFrom(userId: Notification.Notifies, thread: PrivateMessage.Thread): Fu[Boolean] =
    coll.exists($doc(
      "notifies" -> userId,
      "content.type" -> "privateMessage",
      "content.thread.id" -> thread.id
    ) ++ hasOld)

  def hasRecentQaAnswer(userId: Notification.Notifies, questionId: QaAnswer.QuestionId) : Fu[Boolean] = {
    coll.exists($doc(
      "notifies" -> userId,
      "content.type" -> "qaAnswer",
      "content.questionId" -> questionId
    ) ++ hasOldOrUnread)
  }

  val recentSort = $sort desc "createdAt"

  def userNotificationsQuery(userId: Notification.Notifies) = $doc("notifies" -> userId)

  private def unreadOnlyQuery(userId: Notification.Notifies) = $doc("notifies" -> userId, "read" -> false)

}
