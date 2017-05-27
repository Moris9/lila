package lila.chat

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.common.String.html.{ escape => escapeHtml }
import play.api.libs.json._

object JsonView {

  def apply(chat: AnyChat, mobileEscape: Boolean = false): JsValue = {
    if (mobileEscape) escapeHtmlForMobile(chat)
    else chat
  } match {
    case c: MixedChat => mixedChatWriter writes c
    case c: UserChat => userChatWriter writes c
  }

  private def escapeHtmlForMobile(chat: AnyChat) = chat match {
    case c: MixedChat => c.mapLines {
      case l: UserLine => l.copy(text = escapeHtml(l.text))
      case l: PlayerLine => l.copy(text = escapeHtml(l.text))
    }
    case c: UserChat => c.mapLines { l =>
      l.copy(text = escapeHtml(l.text))
    }
  }

  def apply(line: Line): JsValue = lineWriter writes line

  def userModInfo(u: UserModInfo)(implicit lightUser: LightUser.GetterSync) =
    lila.user.JsonView.modWrites.writes(u.user) ++ Json.obj(
      "history" -> u.history
    )

  lazy val timeoutReasons = Json toJson ChatTimeout.Reason.all

  implicit val timeoutReasonWriter: Writes[ChatTimeout.Reason] = OWrites[ChatTimeout.Reason] { r =>
    Json.obj("key" -> r.key, "name" -> r.name)
  }

  implicit def timeoutEntryWriter(implicit lightUser: LightUser.GetterSync) = OWrites[ChatTimeout.UserEntry] { e =>
    Json.obj(
      "reason" -> e.reason.key,
      "mod" -> lightUser(e.mod).fold("?")(_.name),
      "date" -> e.createdAt
    )
  }

  implicit val mixedChatWriter: Writes[MixedChat] = Writes[MixedChat] { c =>
    JsArray(c.lines map lineWriter.writes)
  }

  implicit val userChatWriter: Writes[UserChat] = Writes[UserChat] { c =>
    JsArray(c.lines map userLineWriter.writes)
  }

  private[chat] implicit val lineWriter: Writes[Line] = Writes[Line] {
    case l: UserLine => userLineWriter writes l
    case l: PlayerLine => playerLineWriter writes l
  }

  private implicit val userLineWriter = Writes[UserLine] { l =>
    Json.obj(
      "u" -> l.username,
      "t" -> l.text,
      "r" -> l.troll.option(true),
      "d" -> l.deleted.option(true)
    ).noNull
  }

  private implicit val playerLineWriter = Writes[PlayerLine] { l =>
    Json.obj(
      "c" -> l.color.name,
      "t" -> l.text
    )
  }
}
