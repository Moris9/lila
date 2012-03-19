package lila.http

import play.api.data._
import play.api.data.Forms._

object DataForm {

  type MoveData = (String, String, Option[String], Option[Int])
  val moveForm = Form(tuple(
    "from" -> nonEmptyText,
    "to" -> nonEmptyText,
    "promotion" -> optional(text),
    "b" -> optional(number)
  ))

  type TalkData = (String, String)
  val talkForm = Form(tuple(
    "author" -> nonEmptyText,
    "message" -> nonEmptyText
  ))

  type JoinData = (String, String)
  val joinForm = Form(tuple(
    "redirect" -> nonEmptyText,
    "messages" -> nonEmptyText
  ))

  type RematchData = (String, String)
  val rematchForm = Form(tuple(
    "whiteRedirect" -> nonEmptyText,
    "blackRedirect" -> nonEmptyText
  ))

  private type MessagesData = String
  private val messagesForm = Form(single(
    "messages" -> nonEmptyText
  ))

  type EndData = MessagesData
  val endForm = messagesForm

  type DrawData = MessagesData
  val drawForm = messagesForm
}
