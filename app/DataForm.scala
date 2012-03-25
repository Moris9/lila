package lila.http

import lila.system.model._
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

  type EntryData = String
  val entryForm = Form(single(
    "entry" -> nonEmptyText
  ))

  type JoinData = (String, String, EntryData)
  val joinForm = Form(tuple(
    "redirect" -> nonEmptyText,
    "messages" -> nonEmptyText,
    "entry" -> nonEmptyText
  ))

  type RematchData = (String, String, EntryData)
  val rematchForm = Form(tuple(
    "whiteRedirect" -> nonEmptyText,
    "blackRedirect" -> nonEmptyText,
    "entry" -> nonEmptyText
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
