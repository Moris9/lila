package lila.i18n

import lila.common.Lang

object LangList {

  def name(lang: Lang): String = all.getOrElse(lang, lang.code)

  def nameByStr(str: String): String = I18nLangPicker.byStr(str).fold(str)(name)

  lazy val choices: List[(String, String)] = all.toList.map {
    case (l, name) => l.language -> name
  }.sortBy(_._1)

  private[i18n] val all = Map(
    Lang("en", "GB") -> "English",
    Lang("af", "ZA") -> "Afrikaans",
    Lang("ar", "SA") -> "العربية",
    Lang("as", "IN") -> "অসমীয়া",
    Lang("az", "AZ") -> "Azərbaycanca",
    Lang("be", "BY") -> "Беларуская",
    Lang("bg", "BG") -> "български език",
    Lang("bn", "BD") -> "বাংলা",
    Lang("bs", "BA") -> "bosanski",
    Lang("ca", "ES") -> "Català, valencià",
    Lang("cs", "CZ") -> "čeština",
    Lang("cv", "CU") -> "чӑваш чӗлхи",
    Lang("cy", "GB") -> "Cymraeg",
    Lang("da", "DK") -> "Dansk",
    Lang("de", "DE") -> "Deutsch",
    Lang("el", "GR") -> "Ελληνικά",
    Lang("en", "US") -> "English (US)",
    Lang("eo", "UY") -> "Esperanto",
    Lang("es", "ES") -> "español, castellano",
    Lang("et", "EE") -> "eesti keel",
    Lang("eu", "ES") -> "Euskara",
    Lang("fa", "IR") -> "فارسی",
    Lang("fi", "FI") -> "suomen kieli",
    Lang("fo", "FO") -> "føroyskt",
    Lang("fr", "FR") -> "français",
    Lang("frp", "IT") -> "arpitan",
    Lang("fy", "NL") -> "Frysk",
    Lang("ga", "IE") -> "Gaeilge",
    Lang("gd", "GB") -> "Gàidhlig",
    Lang("gl", "ES") -> "Galego",
    Lang("gu", "IN") -> "ગુજરાતી",
    Lang("he", "IL") -> "עִבְרִית",
    Lang("hi", "IN") -> "हिन्दी, हिंदी",
    Lang("hr", "HR") -> "hrvatski",
    Lang("hu", "HU") -> "Magyar",
    Lang("hy", "AM") -> "Հայերեն",
    Lang("ia", "IA") -> "Interlingua",
    Lang("id", "ID") -> "Bahasa Indonesia",
    Lang("io", "IO") -> "Ido",
    Lang("is", "IS") -> "Íslenska",
    Lang("it", "IT") -> "Italiano",
    Lang("ja", "JP") -> "日本語",
    Lang("jbo", "EN") -> "lojban",
    Lang("jv", "ID") -> "basa Jawa",
    Lang("ka", "GE") -> "ქართული",
    Lang("kab", "KAB") -> "Taqvaylit",
    Lang("kk", "KZ") -> "қазақша",
    Lang("kn", "IN") -> "ಕನ್ನಡ",
    Lang("ko", "KR") -> "한국어",
    Lang("ky", "KG") -> "кыргызча",
    Lang("la", "LA") -> "lingua Latina",
    Lang("lt", "LT") -> "lietuvių kalba",
    Lang("lv", "LV") -> "latviešu valoda",
    Lang("mg", "MG") -> "fiteny malagasy",
    Lang("mk", "MK") -> "македонски јази",
    Lang("ml", "IN") -> "മലയാളം",
    Lang("mn", "MN") -> "монгол",
    Lang("mr", "IN") -> "मराठी",
    Lang("nb", "NO") -> "Norsk bokmål",
    Lang("nl", "NL") -> "Nederlands",
    Lang("nn", "NO") -> "Norsk nynorsk",
    Lang("pi", "IN") -> "पालि",
    Lang("pl", "PL") -> "polski",
    Lang("ps", "AF") -> "پښتو",
    Lang("pt", "PT") -> "Português",
    Lang("pt", "BR") -> "Português (BR)",
    Lang("ro", "RO") -> "Română",
    Lang("ru", "RU") -> "русский язык",
    Lang("sa", "IN") -> "संस्कृत",
    Lang("sk", "SK") -> "slovenčina",
    Lang("sl", "SI") -> "slovenščina",
    Lang("sq", "AL") -> "Shqip",
    Lang("sr", "SP") -> "Српски језик",
    Lang("sv", "SE") -> "svenska",
    Lang("sw", "KE") -> "Kiswahili",
    Lang("ta", "IN") -> "தமிழ்",
    Lang("tg", "TJ") -> "тоҷикӣ",
    Lang("th", "TH") -> "ไทย",
    Lang("tk", "TM") -> "Türkmençe",
    Lang("tl", "PH") -> "Tagalog",
    Lang("tp", "TP") -> "toki pona",
    Lang("tr", "TR") -> "Türkçe",
    Lang("uk", "UA") -> "українська",
    Lang("ur", "IN") -> "اُردُو",
    Lang("uz", "UZ") -> "oʻzbekcha",
    Lang("vi", "VN") -> "Tiếng Việt",
    Lang("yo", "NG") -> "Yorùbá",
    Lang("zh", "CN") -> "中文",
    Lang("zh", "TW") -> "繁體中文",
    Lang("zu", "ZA") -> "isiZulu"
  )
}
