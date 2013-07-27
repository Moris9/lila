package lila.report

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import lila.user.{ User, UserRepo }

private[report] final class DataForm(val captcher: lila.hub.ActorLazyRef) extends lila.hub.CaptchedForm {

  val create = Form(mapping(
    "username" -> nonEmptyText.verifying("Unknown username", { fetchUser(_).isDefined }),
    "text" -> text(minLength = 5, maxLength = 2000),
    "gameId" -> text,
    "move" -> text
  )({
      case (username, text, gameId, move) ⇒ ReportSetup(
        user = fetchUser(username) err "Unknown username " + username,
        text = text,
        gameId = gameId,
        move = move)
    })(_.export.some)
    .verifying(captchaFailMessage, validateCaptcha _))

  def createWithCaptcha = withCaptcha(create)

  private def fetchUser(username: String) = (UserRepo named username).await
}

private[report] case class ReportSetup(
    user: User,
    text: String,
    gameId: String,
    move: String) {

  def export = (user.username, text, gameId, move)
}
