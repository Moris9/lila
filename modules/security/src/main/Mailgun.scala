package lila.security

import scala.concurrent.duration._

import akka.actor.ActorSystem
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current
import scalatags.Text.all._

import lila.common.String.html.escapeHtml
import lila.common.{ Lang, EmailAddress }
import lila.i18n.I18nKeys.{ emails => trans }

final class Mailgun(
    apiUrl: String,
    apiKey: String,
    from: String,
    replyTo: String,
    system: ActorSystem
) {

  def send(msg: Mailgun.Message): Funit =
    if (apiUrl.isEmpty) {
      println(msg, "No mailgun API URL")
      funit
    } else {
      lila.mon.email.actions.send()
      WS.url(s"$apiUrl/messages").withAuth("api", apiKey, WSAuthScheme.BASIC).post(Map(
        "from" -> Seq(msg.from | from),
        "to" -> Seq(msg.to.value),
        "h:Reply-To" -> Seq(msg.replyTo | replyTo),
        "o:tag" -> msg.tag.toSeq,
        "subject" -> Seq(msg.subject),
        "text" -> Seq(msg.text)
      ) ++ msg.htmlBody.?? { body =>
          Map("html" -> Seq(Mailgun.html.wrap(msg.subject, body).render))
        }).void addFailureEffect {
        case e: java.net.ConnectException => lila.mon.http.mailgun.timeout()
        case _ =>
      } recoverWith {
        case e if msg.retriesLeft > 0 => {
          lila.mon.email.actions.retry()
          akka.pattern.after(15 seconds, system.scheduler) {
            send(msg.copy(retriesLeft = msg.retriesLeft - 1))
          }
        }
        case e => {
          lila.mon.email.actions.fail()
          fufail(e)
        }
      }
    }
}

object Mailgun {

  case class Message(
      to: EmailAddress,
      subject: String,
      text: String,
      htmlBody: Option[Frag] = none,
      from: Option[String] = none,
      replyTo: Option[String] = none,
      tag: Option[String] = none,
      retriesLeft: Int = 3
  )

  object txt {

    def serviceNote(implicit lang: Lang) = s"""
${trans.common_note.literalTo(lang, List("https://lichess.org")).render}

${trans.common_contact.literalTo(lang, List("https://lichess.org/contact")).render}"""
  }

  object html {

    val noteLink = raw {
      """<a itemprop="url" href="https://lichess.org/"><span itemprop="name">lichess.org</span></a>"""
    }
    val noteContact = raw {
      """<a itemprop="url" href="https://lichess.org/contact"><span itemprop="name">lichess.org/contact</span></a>"""
    }

    def serviceNote(implicit lang: Lang) = s"""
<div itemprop="publisher" itemscope itemtype="http://schema.org/Organization">
  <small>${trans.common_note.literalTo(lang, List(noteLink)).render} ${trans.common_contact.literalTo(lang, List(noteContact)).render}</small>
</div>
"""

    def url(u: String)(implicit lang: Lang) = s"""
<meta itemprop="url" content="$u">
<p><a itemprop="target" href="$u">$u</a></p>
<p>${trans.common_orPaste.literalTo(lang).render}</p>
"""

    private[Mailgun] def wrap(subject: String, body: Frag) = raw {
      s"""<!doctype html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <title>${escapeHtml(subject)}</title>
  </head>
  <body>
    $body
  </body>
</html>"""
    }
  }
}
