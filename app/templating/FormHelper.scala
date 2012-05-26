package lila
package templating

import play.api.data._
import play.api.templates.Html

trait FormHelper {

  private val errNames = Map(
    "error.minLength" -> "Text is too short."
  )

  def errMsg(form: Field): Html = Html {
    form.errors map { e ⇒ 
    """<p class="error">%s</p>""".format(
      (errNames get e.message) | e.message)
    } mkString
  }
}
