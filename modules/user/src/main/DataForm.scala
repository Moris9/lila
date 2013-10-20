package lila.user

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val bio = Form(single(
    "bio" -> text(maxLength = 400)
  ))

  val profile = Form(mapping(
    "firstName" -> nameField,
    "lastName" -> nameField,
    "country" -> optional(nonEmptyText.verifying(Countries.codeSet contains _)),
    "bio" -> optional(nonEmptyText(maxLength = 400))
  )(Profile.apply)(Profile.unapply))

  private def nameField = optional(nonEmptyText(minLength = 2, maxLength = 20))

  val theme = Form(single(
    "theme" -> nonEmptyText.verifying(Theme contains _)
  ))

  val bg = Form(single(
    "bg" -> text.verifying(Set("light", "dark") contains _)
  ))

  case class Passwd(
      oldPasswd: String,
      newPasswd1: String,
      newPasswd2: String) {
    def samePasswords = newPasswd1 == newPasswd2
  }

  val passwd = Form(mapping(
    "oldPasswd" -> nonEmptyText,
    "newPasswd1" -> nonEmptyText(minLength = 2),
    "newPasswd2" -> nonEmptyText(minLength = 2)
  )(Passwd.apply)(Passwd.unapply).verifying(
      "the new passwords don't match",
      _.samePasswords
    ))

  private def jsBoolean = nonEmptyText.verifying(Set("true", "false") contains _)
}
