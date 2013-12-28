package lila.chat

import java.util.regex.Matcher.quoteReplacement

import Line.{ BSONFields ⇒ L }
import org.apache.commons.lang3.StringEscapeUtils.escapeXml
import play.api.libs.json._

import lila.db.api._
import lila.db.Implicits._
import lila.user.{ User, UserRepo }
import tube.lineTube

private[chat] final class Api(
    flood: lila.security.Flood,
    prefApi: lila.pref.PrefApi,
    netDomain: String) {

  def get(user: User): Fu[Chat] = prefApi getPref user flatMap { pref ⇒
    val chans = Chat.baseChans map { chan ⇒
      chan -> (pref.chat.chans contains chan.key)
    }
    val mainChan = pref.chat.mainChan flatMap Chan.parse
    val selectTroll = user.troll.fold(Json.obj(), Json.obj(L.troll -> false))
    $find($query(selectTroll ++ Json.obj("c" -> $in(pref.chat.chans))) sort $sort.desc(L.date), 20) map { lines ⇒
      Chat(user, lines.reverse, chans, mainChan)
    }
  }

  def write(chan: String, userId: String, text: String): Fu[Option[Line]] = {
    import Writer._
    UserRepo byId userId flatMap {
      case None ⇒ invalid(s"$userId does not even exist")
      case Some(user) ⇒ Chan parse chan match {
        case None    ⇒ invalid(s"Invalid chan name $chan")
        case Some(c) ⇒ write(c, user, text)
      }
    }
  }

  def write(chan: Chan, user: User, t1: String): Fu[Option[Line]] = {
    import Writer._
    if (user.disabled) invalid(s"User $user is disabled and can't write in the $chan chat")
    else {
      val t2 = t1.trim take 200
      if (t2.isEmpty) invalid(s"Empty message from $user in $chan")
      else {
        val text = delocalize(noPrivateUrl(t2))
        val line = Line.make(chan, user, text)
        if (flood.allowMessage(line.userId, line.text))
          $insert bson line inject line.some
        else
          invalid(s"$user is flooding $chan: ${line.text.take(40)}")
      }
    }
  }

  private val logger = play.api.Logger("chat")

  private object Writer {
    def invalid(msg: String) = { logger.info(msg); fuccess(none[Line]) }

    val delocalize = new lila.common.String.Delocalizer(netDomain)
    val domainRegex = netDomain.replace(".", """\.""")
    val urlRegex = (domainRegex + """/([\w-]{8})[\w-]{4}""").r
    def noPrivateUrl(str: String): String =
      urlRegex.replaceAllIn(str, m ⇒ quoteReplacement(netDomain + "/" + (m group 1)))
  }
}
