package lila
package wiki

import user.User

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import java.text.Normalizer
import java.util.regex.Matcher.quoteReplacement

case class Page(
  @Key("_id") id: String,
  slug: String,
  number: Int,
  lang: String,
  title: String,
  body: String)

object Page {

  val NameRegex = """^(\w{2,3})_(\d+)_(.+)$""".r

  // name = en_1_Some Title
  def apply(name: String, body: String): Option[Page] = name match {
    case NameRegex(lang, numberStr, title) ⇒
      parseIntOption(numberStr) map { number ⇒
        Page(
          id = name,
          number = number,
          slug = slugify(title),
          lang = lang,
          title = title.replace("-", " "),
          body = body)
      }
    case _ ⇒ none
  }

  private def slugify(input: String) = {
    val nowhitespace = input.replace(" ", "_")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    """[^\w-]""".r.replaceAllIn(normalized, "")
  }
}
