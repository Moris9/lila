package lila

import play.api.i18n.Lang

package object i18n extends PackageObject {

  type Count = Int
  type MessageKey = String

  /* Implemented by mutable.AnyRefMap.
   * Of course we don't need/use the mutability;
   * it's just that AnyRefMap is the fastest scala hashmap implementation
   */
  private[i18n] type MessageMap = scala.collection.Map[MessageKey, Translation]
  private[i18n] type Messages = Map[Lang, MessageMap]

  private[i18n] def logger = lila.log("i18n")

  private[i18n] val lichessCodes: Map[String, Lang] = Map(
    "fp" -> Lang("frp", "IT"),
    "jb" -> Lang("jbo", "EN"),
    "kb" -> Lang("kab", "KAB"),
    "tc" -> Lang("zh", "CN")
  )

  val enLang = Lang("en", "GB")
  val defaultLang = enLang
}
