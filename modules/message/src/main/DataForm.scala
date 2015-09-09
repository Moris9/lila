package lila.message

import play.api.data._
import play.api.data.Forms._

import lila.user.{ User, UserRepo }

private[message] final class DataForm(security: MessageSecurity) {

  import DataForm._

  def thread(me: User) = Form(mapping(
    "username" -> nonEmptyText(maxLength = 20)
      .verifying("Unknown username", { fetchUser(_).isDefined })
      .verifying("Sorry, this player doesn't accept new messages", { name =>
        security.canMessage(me.id, User normalize name).await // damn you blocking API
      }),
    "subject" -> text(minLength = 3, maxLength = 100),
    "text" -> text(minLength = 3, maxLength = 8000)
  )({
      case (username, subject, text) => ThreadData(
        user = fetchUser(username) err "Unknown username " + username,
        subject = subject,
        text = text)
    })(_.export.some))

  def post = Form(single(
    "text" -> text(minLength = 3)
  ))

  private def fetchUser(username: String) = (UserRepo named username).await
}

object DataForm {

  case class ThreadData(
      user: User,
      subject: String,
      text: String) {

    def export = (user.username, subject, text)
  }
}
