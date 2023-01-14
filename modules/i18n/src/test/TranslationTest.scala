package lila.i18n

import org.specs2.mutable.Specification
import scala.jdk.CollectionConverters._

class TranslationTest extends Specification {

  "translations" >> {
    "be valid" >> {
      val en     = Registry.all.get(defaultLang).get
      var tested = 0
      val errors: List[String] = LangList.all.flatMap { case (lang, name) =>
        given play.api.i18n.Lang = lang
        Registry.all.get(lang).get.asScala.toMap flatMap { case (k, v) =>
          try {
            val enTrans: String = en.get(k) match {
              case literal: Simple  => literal.message
              case literal: Escaped => literal.message
              case plurals: Plurals => plurals.messages(I18nQuantity.Other)
            }
            val args = argsForKey(enTrans)
            v match {
              case literal: Simple =>
                tested = tested + 1
                literal.formatTxt(args)
              case literal: Escaped =>
                tested = tested + 1
                literal.formatTxt(args)
              case plurals: Plurals =>
                plurals.messages.keys foreach { qty =>
                  tested = tested + 1
                  plurals.formatTxt(qty, args) must beSome
                }
            }
            None
          } catch {
            case _: MatchError => None // Extra translation
            case e: Exception  => Some(s"${lang.code} $name $k -> $v - ${e.getMessage}")
          }
        }
      }.toList
      println(s"$tested translations tested")
      if (errors.isEmpty) success
      else failure(errors mkString "\n")
    }
    "escape html" >> {
      import play.api.i18n.Lang
      import scalatags.Text.all.*
      given lang: Lang = defaultLang
      I18nKeys.depthX("string") === RawFrag("Depth string")
      I18nKeys.depthX("<string>") === RawFrag("Depth &lt;string&gt;")
      I18nKeys.depthX(Html("<html>")) === RawFrag("Depth &lt;html&gt;")
    }
  }

  private def argsForKey(k: String): List[String] =
    if (k contains "%s") List("arg1")
    else if (k contains "%4$s") List("arg1", "arg2", "arg3", "arg4")
    else if (k contains "%3$s") List("arg1", "arg2", "arg3")
    else if (k contains "%2$s") List("arg1", "arg2")
    else if (k contains "%1$s") List("arg1")
    else Nil
}
