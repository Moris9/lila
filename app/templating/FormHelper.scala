package lila.app
package templating

import play.api.data._
import play.twirl.api.Html

import lila.api.Context
import lila.i18n.I18nDb

trait FormHelper { self: I18nHelper =>

  def errMsg(form: Field)(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(form: Form[_])(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(error: FormError)(implicit ctx: Context): Html = Html {
    s"""<p class="error">${transKey(error.message, I18nDb.Site, error.args)}</p>"""
  }

  def errMsg(errors: Seq[FormError])(implicit ctx: Context): Html = Html {
    errors map errMsg mkString
  }

  def errMsgMaterial(errors: Seq[FormError])(implicit ctx: Context): Option[Html] = errors.nonEmpty option Html {
    val msgs = errors.map { error =>
      s"""<p class="error">${transKey(error.message, I18nDb.Site, error.args)}</p>"""
    } mkString ""
    s"""<div class="form-group has-error">$msgs</div>"""
  }

  def globalError(form: Form[_])(implicit ctx: Context): Option[Html] =
    form.globalError map errMsg

  def globalErrorMaterial(form: Form[_])(implicit ctx: Context): Option[Html] =
    form.globalError map { msg =>
      Html(s"""<div class="form-group has-error">${errMsg(msg)}</div>""")
    }

  val booleanChoices = Seq("true" -> "✓ Yes", "false" -> "✗ No")

  object form3 {

    val idPrefix = "form3"

    def id(field: Field) = s"$idPrefix-${field.id}"

    def group(
      field: Field,
      name: Html,
      klass: String = "",
      half: Boolean = false,
      help: Option[Html] = None
    )(html: Field => Html)(implicit ctx: Context) = Html {
      val classes = s"""form-group${field.hasErrors ?? " is-invalid"}${half ?? " half"} $klass"""
      val label = s"""<label for="${id(field)}">$name</label>"""
      val helper = help ?? { h => s"""<small class="form-help">$h</small>""" }
      s"""<div class="$classes">$label${html(field)}${errMsg(field)}$helper</div>"""
    }

    def select(
      field: Field,
      options: Iterable[(Any, String)],
      default: Option[String] = None
    ) = Html {
      val defaultH = default ?? { d => s"""<option value="">$d</option>""" }
      val optionsH = options map { v =>
        s"""<option value="${v._1}" ${(field.value == Some(v._1.toString)) ?? "selected"}>${v._2}</option>"""
      } mkString ""
      s"""<select id="${id(field)}" name="${field.name}" class="form-control">$defaultH$optionsH</select>"""
    }

    def textarea(
      field: Field,
      klass: String = "",
      required: Boolean = false,
      rows: Option[Int] = None,
      attrs: String = ""
    ) = Html {
      val rowsH = rows ?? { r => s""" rows=$r""" }
      s"""<textarea id="${id(field)}" name="${field.name}" class="form-control $klass"$rowsH${required ?? " required"}$attrs>${~field.value}</textarea>"""
    }

    def actions(html: Html) = Html {
      s"""<div class="form-actions">$html</div>"""
    }

    def submit(
      content: Html,
      icon: Option[String] = Some("E"),
      nameValue: Option[(String, String)] = None
    ) = Html {
      val iconH = icon ?? { i => s""" data-icon="$i"""" }
      val nameH = nameValue ?? { case (n, v) => s""" name="$n" value="$v"""" }
      s"""<button class="submit button${icon.isDefined ?? " text"} type="submit"$iconH$nameH>$content</button>"""
    }

    def hidden(field: Field, value: Option[String] = None) = Html {
      s"""<input type="hidden" name="${field.name}" id="${id(field)}" value="${value | ~field.value}"/>"""
    }

    def input(
      field: Field,
      typ: String = "text",
      klass: String = "",
      placeholder: Option[String] = None,
      required: Boolean = false,
      minLength: Int = 0,
      maxLength: Int = 0,
      autocomplete: Boolean = true,
      autofocus: Boolean = false,
      pattern: Option[String] = None,
      attrs: String = ""
    ) = Html {
      val options = s"""${placeholder ?? { p => s""" placeholder="$p"""" }}${required ?? " required"}${(minLength > 0) ?? s"minlength=$minLength"}${(maxLength > 0) ?? s"maxlength=$maxLength"}${!autocomplete ?? """autocomplete="off""""}${autofocus ?? " autofocus"}${pattern ?? { p => s""" pattern="$p"""" }} $attrs"""
      s"""<input type="typ" id="${id(field)}" name="${field.name}" value="${~field.value}"$options class="form-control $klass"/>"""
    }
  }
}
