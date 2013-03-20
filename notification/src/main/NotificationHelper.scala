package lila.notification

import lila.user.{ User, UserHelper }
import lila.notification.Env.{ current ⇒ notificationEnv }

import play.api.templates.Html
import play.api.mvc.Call

trait NotificationHelper {

  def notifications(user: User): Html = {
    val notifs = notificationEnv.api get user.id take 2 map { notif ⇒
      views.html.notification.view(notif.id, notif.from)(Html(notif.html))
    }
    notifs.foldLeft(Html(""))(_ += _)
  }
}
