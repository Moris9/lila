package lila.app
package templating

import java.util.Locale
import scala.collection.mutable

import org.joda.time.DateTime
import org.joda.time.format._
import play.api.templates.Html

import lila.user.Context

trait DateHelper { self: I18nHelper ⇒

  private val style = "MS"

  private val formatters = mutable.Map[String, DateTimeFormatter]()

  private val isoFormatter = ISODateTimeFormat.dateTime

  private def formatter(ctx: Context): DateTimeFormatter =
    formatters.getOrElseUpdate(
      lang(ctx).language,
      DateTimeFormat forStyle style withLocale new Locale(lang(ctx).language))

  def showDate(date: DateTime)(implicit ctx: Context): String =
    formatter(ctx) print date

  def timeago(date: DateTime)(implicit ctx: Context): Html = Html(
    """<time class="timeago" datetime="%s">%s</time>"""
      .format(isoFormatter print date, showDate(date))
  )

  def timeagoLocale(implicit ctx: Context): Option[String] =
    lang(ctx).language match {
      case "en" ⇒ none
      case "pt" ⇒ "pt-br".some
      case "zh" ⇒ "zh-CN".some
      case l    ⇒ timeagoLocales(l) option l
    }

  private lazy val timeagoLocales: Set[String] = {
    import java.io.File
    val Regex = """^jquery\.timeago\.(\w{2})\.js$""".r
    (new File(Env.current.timeagoLocalesPath).listFiles map (_.getName) collect {
      case Regex(l) ⇒ l
    }).toSet: Set[String]
  }
}
