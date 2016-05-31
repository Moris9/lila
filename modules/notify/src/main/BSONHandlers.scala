package lila.notify

import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._
import lila.db.{ dsl, BSON }
import lila.notify.InvitedToStudy.{ StudyName, InvitedBy, StudyId }
import lila.notify.MentionedInThread._
import lila.notify.Notification._
import reactivemongo.bson.Macros
import reactivemongo.bson.{ BSONString, BSONHandler, BSONDocument }

private object BSONHandlers {

  implicit val NotifiesHandler = stringAnyValHandler[Notifies](_.value, Notifies.apply)

  implicit val MentionByHandler = stringAnyValHandler[MentionedBy](_.value, MentionedBy.apply)
  implicit val TopicHandler = stringAnyValHandler[Topic](_.value, Topic.apply)
  implicit val TopicIdHandler = stringAnyValHandler[TopicId](_.value, TopicId.apply)
  implicit val CategoryHandler = stringAnyValHandler[Category](_.value, Category.apply)
  implicit val PostIdHandler = stringAnyValHandler[PostId](_.value, PostId.apply)

  implicit val InvitedToStudyByHandler = stringAnyValHandler[InvitedBy](_.value, InvitedBy.apply)
  implicit val StudyNameHandler = stringAnyValHandler[StudyName](_.value, StudyName.apply)
  implicit val StudyIdHandler = stringAnyValHandler[StudyId](_.value, StudyId.apply)
  import Notification.{ Notifies, NotificationRead }
  implicit val ReadHandler = booleanAnyValHandler[NotificationRead](_.value, NotificationRead.apply)

  implicit val NotificationContentHandler = new BSON[NotificationContent] {

    private def writeNotificationType(notificationContent: NotificationContent) = {
      notificationContent match {
        case _: MentionedInThread => "mention"
        case _: InvitedToStudy    => "invitedStudy"
      }
    }

    private def writeNotificationContent(notificationContent: NotificationContent) = {
      notificationContent match {
        case MentionedInThread(mentionedBy, topic, topicId, category, postId) =>
          $doc("type" -> writeNotificationType(notificationContent), "mentionedBy" -> mentionedBy,
            "topic" -> topic, "topicId" -> topicId, "category" -> category, "postId" -> postId)
        case InvitedToStudy(invitedBy, studyName, studyId) =>
          $doc("type" -> writeNotificationType(notificationContent),
            "invitedBy" -> invitedBy,
            "studyName" -> studyName,
            "studyId" -> studyId)
      }
    }

    private def readMentionedNotification(reader: Reader): MentionedInThread = {
      val mentionedBy = reader.get[MentionedBy]("mentionedBy")
      val topic = reader.get[Topic]("topic")
      val topicId = reader.get[TopicId]("topicId")
      val category = reader.get[Category]("category")
      val postNumber = reader.get[PostId]("postId")

      MentionedInThread(mentionedBy, topic, topicId, category, postNumber)
    }

    private def readInvitedStudyNotification(reader: Reader): NotificationContent = {
      val invitedBy = reader.get[InvitedBy]("invitedBy")
      val studyName = reader.get[StudyName]("studyName")
      val studyId = reader.get[StudyId]("studyId")

      InvitedToStudy(invitedBy, studyName, studyId)
    }

    def reads(reader: Reader): NotificationContent = {
      val notificationType = reader.str("type")

      notificationType match {
        case "mention"      => readMentionedNotification(reader)
        case "invitedStudy" => readInvitedStudyNotification(reader)
      }
    }

    def writes(writer: Writer, n: NotificationContent): dsl.Bdoc = {
      writeNotificationContent(n)
    }
  }

  implicit val NotificationBSONHandler = Macros.handler[Notification]
}
