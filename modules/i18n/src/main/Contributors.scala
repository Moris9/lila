package lila.i18n

private[i18n] object Contributors {

  val all = Map(
    "fr" -> List("Thibault Duplessis"),
    "ru" -> List("Nikita Milovanov"),
    "de" -> List("Patrick Gawliczek", "Kurt Keller (DE)"),
    "tr" -> List("Yakup Ipek"),
    "sr" -> List("Nenad Nikolić"),
    "lv" -> List("Anonymous"),
    "bs" -> List("Jacikka"),
    "da" -> List("Henrik Bjornskov", "Kurt Keller (DE)"),
    "es" -> List("FennecFoxz"),
    "ro" -> List("Cristian Niţă"),
    "it" -> List("Marco Barberis"),
    "fi" -> List("Juuso Vallius"),
    "uk" -> List("alterionisto"),
    "pt" -> List("Arthur Braz", "Eugênio Vázquez"),
    "pl" -> List("M3n747", "Kurt Keller (DE)"),
    "nl" -> List("Kintaro"),
    "vi" -> List("Xiblack"),
    "sv" -> List("nizleib", "Kurt Keller (DE)"),
    "cs" -> List("Martin", "Claymes"),
    "sk" -> List("taiga", "Kurt Keller (DE)"),
    "hu" -> List("LTBakemono"),
    "ca" -> List("AI8"),
    "sl" -> List("Klemen Grm"),
    "az" -> List("elçin məmmədzadə", "amil isgəndərov"),
    "nn" -> List("Peropaal"),
    "eo" -> List("LaPingvino"),
    "tp" -> List("jan Mimoku"),
    "el" -> List("Tzortzakos Fivos", "Γιάννης Ανθυμίδης"),
    "fp" -> List("Alex"),
    "lt" -> List("Anonymous"),
    "nb" -> List("sundaune"),
    "et" -> List("Anonymous"),
    "hy" -> List("Network.am"),
    "af" -> List("secreteagle"),
    "hi" -> List("Samarth Karia"),
    "ar" -> List("Ziad Dabash"),
    "zh" -> List("神爱"),
    "gl" -> List("José Manuel Castroagudín Silva"),
    "tk" -> List("Anonymous"),
    "hr" -> List("Betyárcsimbók"),
    "mk" -> List("Давид и Стефан Тимовски"),
    "id" -> List("Night1301"),
    "ja" -> List("ネイサン　アイブス"),
    "bg" -> List("Anonymous", "Пламен Димов."),
    "th" -> List("มาโนชญ์ สมศักดิ์"),
    "fa" -> List("saeid monajiane"),
    "he" -> List("Anonymous", " Tornado"),
    "mr" -> List("Rahul"),
    "mn" -> List("Tsbarsaa"),
    "cy" -> List("cavejohnson"),
    "gd" -> List("GunChleoc"),
    "ga" -> List("Anonymous"),
    "sq" -> List("Indrit Bleta"),
    "be" -> List("Palenik Siarhei"),
    "ka" -> List("Giorgi Javakhidze"),
    "sw" -> List("Anonymous"),
    "ps" -> List("Eimal Dorani"),
    "is" -> List("Sir Gizmo Gunn Myr Basque", "cyberpunk"),
    "kk" -> List("Arsakay Madi"),
    "io" -> List("Fabian Mokross"),
    "gu" -> List("Anonymous"),
    "fo" -> List("Anonymous"),
    "eu" -> List("Anonymous"),
    "bn" -> List("Ankit Peet"),
    "id" -> List("KenXeiko"),
    "la" -> List("3_1415maldaumen"),
    "jv" -> List("errorfilename"),
    "ky" -> List("tmed"),
    "pi" -> List("novalis78"))

  def apply(code: String): List[String] = ~(all get code)
}
