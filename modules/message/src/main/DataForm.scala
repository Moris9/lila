package lila.message

import play.api.data._
import play.api.data.Forms._

import lila.user.{ User, UserRepo }

private[message] final class DataForm(blocks: (String, String) ⇒ Fu[Boolean]) {

  import DataForm._

  def thread(me: User) = Form(mapping(
    "username" -> nonEmptyText
      .verifying("Unknown username", { fetchUser(_).isDefined })
      .verifying("This user blocks you", u ⇒ !blocks(u.toLowerCase, me.id).await),
    "subject" -> text(minLength = 3),
    "text" -> text(minLength = 3)
  )({
      case (username, subject, text) ⇒ ThreadData(
        user = fetchUser(username) err "Unknown username " + username,
        subject = subject,
        text = text)
    })(_.export.some))

  val post = Form(single(
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
