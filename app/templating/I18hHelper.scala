package lila.app
package templating

import play.api.libs.json.JsObject
import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nDb, I18nKey, JsDump, LangList, TimeagoLocales, Translator }
import lila.user.UserContext

trait I18nHelper extends HasEnv {

  implicit def ctxLang(implicit ctx: UserContext): Lang = ctx.lang

  def transKey(key: String, db: I18nDb.Ref, args: Seq[Any] = Nil)(implicit lang: Lang): Frag =
    Translator.frag.literal(key, db, args, lang)

  def i18nJsObject(keys: Seq[I18nKey])(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys, I18nDb.Site, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys.flatten, I18nDb.Site, lang)

  def i18nFullDbJsObject(db: I18nDb.Ref)(implicit lang: Lang): JsObject =
    JsDump.dbToObject(db, lang)

  def timeagoLocaleScript(implicit ctx: lila.api.Context): String = {
    TimeagoLocales.js.get(ctx.lang.code) orElse
      TimeagoLocales.js.get(ctx.lang.language) getOrElse
      ~TimeagoLocales.js.get("en")
  }

  def langName = LangList.nameByStr _

  def shortLangName(str: String) = langName(str).takeWhile(',' !=)
}
