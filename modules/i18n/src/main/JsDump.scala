package lila.i18n

import java.io._
import scala.concurrent.Future

import play.api.i18n.Lang
import play.api.libs.json.{ JsString, JsObject }

private[i18n] final class JsDump(path: String) {

  def apply: Funit = Future {
    pathFile.mkdir
    writeRefs
    writeFullJson
  } void

  private val pathFile = new File(path)

  private def dumpFromKey(keys: Iterable[String], lang: Lang): String =
    keys.map { key =>
      """"%s":"%s"""".format(key, escape(Translator.txt.literal(key, I18nDb.Site, Nil, lang)))
    }.mkString("{", ",", "}")

  private def writeRefs = writeFile(
    new File("%s/refs.json".format(pathFile.getCanonicalPath)),
    LangList.all.toList.sortBy(_._1.code).map {
      case (lang, name) => s"""["${lang.code}","$name"]"""
    }.mkString("[", ",", "]")
  )

  private def writeFullJson = I18nDb.langs foreach { lang =>
    val code = dumpFromKey(I18nDb.site(defaultLang).keys, lang)
    val file = new File("%s/%s.all.json".format(pathFile.getCanonicalPath, lang.code))
    writeFile(new File("%s/%s.all.json".format(pathFile.getCanonicalPath, lang.code)), code)
  }

  private def writeFile(file: File, content: String) = {
    val out = new PrintWriter(file)
    try { out.print(content) }
    finally { out.close }
  }

  private def escape(text: String) = text.replace(""""""", """\"""")
}

object JsDump {

  private def quantitySuffix(q: I18nQuantity): String = q match {
    case I18nQuantity.Zero => ":zero"
    case I18nQuantity.One => ":one"
    case I18nQuantity.Two => ":two"
    case I18nQuantity.Few => ":few"
    case I18nQuantity.Many => ":many"
    case I18nQuantity.Other => ""
  }

  private type JsTrans = Iterable[(String, JsString)]

  private def translatedJs(k: String, t: Translation, lang: Lang): JsTrans = t match {
    case literal: Literal => List(k -> JsString(literal.message))
    case plurals: Plurals => plurals.messages.map {
      case (quantity, msg) => k + quantitySuffix(quantity) -> JsString(msg)
    }
  }

  def keysToObject(keys: Seq[I18nKey], db: I18nDb.Ref, lang: Lang): JsObject = JsObject {
    keys.flatMap { k =>
      Translator.findTranslation(k.key, db, lang).fold(Nil: JsTrans) { translatedJs(k.key, _, lang) }
    }
  }

  def dbToObject(ref: I18nDb.Ref, lang: Lang): JsObject = JsObject {
    I18nDb(ref).get(defaultLang) ?? { defaultMsgs =>
      val msgs = ~I18nDb(ref).get(lang)
      defaultMsgs.flatMap {
        case (k, v) => translatedJs(k, msgs get k getOrElse v, lang)
      }
    }
  }
}
