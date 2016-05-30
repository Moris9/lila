package lila.study

import akka.actor._
import akka.pattern.ask

import lila.hub.actorApi.HasUserId
import lila.hub.actorApi.message.LichessThread
import lila.notify.InvitedToStudy.InvitedBy
import lila.notify.{InvitedToStudy, NotifyApi, Notification}
import makeTimeout.short
import org.joda.time.DateTime

private final class StudyNotifier(
    notifyApi: NotifyApi) {

  def apply(study: Study, invited: lila.user.User, socket: ActorRef) =
    socket ? HasUserId(invited.id) mapTo manifest[Boolean] map { isPresent =>
      study.owner.ifFalse(isPresent) foreach { owner =>
        if (!isPresent) {
          val notificationContent = InvitedToStudy(InvitedToStudy.InvitedBy(owner.id), InvitedToStudy.StudyName(study.name), InvitedToStudy.StudyId(study.id))
          val notification = Notification(Notification.Notifies(invited.id), notificationContent, Notification.NotificationRead(false), DateTime.now())
          notifyApi.addNotification(notification)
        }
      }
    }
}
