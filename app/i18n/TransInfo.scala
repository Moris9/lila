package lila
package i18n

import play.api.i18n.{ MessagesApi, Lang }

case class TransInfo(
    lang: Lang,
    name: String,
    contributors: List[String],
    nbTranslated: Int,
    nbMissing: Int) {

  def code = lang.language

  def codeAndName = code + " - " + name

  def nbMessages = nbTranslated + nbMissing

  def percent = nbTranslated * 100 / nbMessages

  def complete = percent == 100

  def nonComplete = !complete
}

case class TransInfos(all: List[TransInfo]) {

  lazy val byCode = all map { info ⇒
    info.code -> info
  } toMap

  def get(code: String): Option[TransInfo] = byCode get code

  def get(lang: Lang): Option[TransInfo] = get(lang.language)
}

object TransInfos {

  val defaultCode = "en"

  def apply(api: MessagesApi, keys: I18nKeys): TransInfos = TransInfos {
    val nbMessages = keys.keys.size
    LangList.sortedList.filter(_._1 != defaultCode) map {
      case (code, name) ⇒ TransInfo(
        lang = Lang(code),
        name = name,
        contributors = Contributors(code),
        nbTranslated = ~(api.messages get code map (_.size)),
        nbMissing = nbMessages - ~(api.messages get code map (_.size))
      )
    }
  }
}
