package lila
package i18n

object LangList {

  def name(code: String) = all get code 

  def nameOrCode(code: String) = name(code) | code

  lazy val sortedList = all.toList sortBy (_._1)

  val all = Map(
    "ab" -> "аҧсуа",
    "aa" -> "Afaraf",
    "af" -> "Afrikaans",
    "ak" -> "Akan",
    "ar" -> "العربية",
    "sq" -> "Shqip",
    "am" -> "አማርኛ",
    "an" -> "Aragonés",
    "hy" -> "Հայերեն",
    "as" -> "অসমীয়া",
    "av" -> "авар мацӀ, магӀарул мацӀ",
    "ae" -> "avesta",
    "ay" -> "aymar aru",
    "az" -> "azərbaycan dili",
    "bm" -> "bamanankan",
    "ba" -> "башҡорт теле",
    "eu" -> "euskara, euskera",
    "be" -> "Беларуская",
    "bn" -> "বাংলা",
    "bh" -> "भोजपुरी",
    "bi" -> "Bislama",
    "bs" -> "bosanski jezik",
    "br" -> "brezhoneg",
    "bg" -> "български език",
    "ca" -> "Català",
    "ch" -> "Chamoru",
    "ce" -> "нохчийн мотт",
    "ny" -> "chiCheŵa, chinyanja",
    "zh" -> "中文",
    "cv" -> "чӑваш чӗлхи",
    "kw" -> " Kernewek",
    "co" -> "corsu, lingua corsa",
    "cr" -> "ᓀᐦᐃᔭᐍᐏᐣ",
    "hr" -> "hrvatski",
    "cs" -> "česky, čeština",
    "da" -> "Dansk",
    "nl" -> "Nederlands, Vlaams",
    "en" -> "English",
    "eo" -> "Esperanto",
    "et" -> "eesti, eesti keel",
    "ee" -> "Eʋegbe",
    "fa" -> "فارسی",
    "fo" -> "føroyskt",
    "fj" -> "vosa Vakaviti",
    "fi" -> "suomi, suomen kieli",
    "fr" -> "français",
    "fp" -> "arpitan",
    //"frp" -> "arpitan",
    "ff" -> "Fulfulde, Pulaar, Pular",
    "gl" -> "Galego",
    "ka" -> "ქართულ",
    "de" -> "Deutsch",
    "el" -> "Ελληνικά",
    "gn" -> "Avañe'ẽ",
    "gu" -> "ગુજરાતી",
    "he" -> "עִבְרִית",
    "ht" -> "Kreyòl ayisyen",
    "hz" -> "Otjiherero",
    "hi" -> "हिन्दी, हिंदी",
    "ho" -> "Hiri Motu",
    "hu" -> "Magyar",
    "ia" -> "Interlingua",
    "id" -> "Bahasa Indonesia",
    "ga" -> "Gaeilge",
    "ig" -> "Asụsụ Igbo",
    "ik" -> "Iñupiaq, Iñupiatun",
    "io" -> "Ido",
    "is" -> "Íslenska",
    "it" -> "Italian",
    "iu" -> "ᐃᓄᒃᑎᑐ",
    "ja" -> "日本語",
    "jv" -> "basa Jaw",
    "kl" -> "kalaallisut, kalaallit oqaasi",
    "kn" -> "ಕನ್ನಡ",
    "kr" -> "Kanuri",
    "kk" -> "Қазақ тіл",
    "km" -> "ភាសាខ្មែ",
    "ki" -> "Gĩkũy",
    "rw" -> "Ikinyarwand",
    "ky" -> "кыргыз тили",
    "kv" -> "коми кыв",
    "kg" -> "KiKongo",
    "kj" -> "Kuanyam",
    "lb" -> "Lëtzebuergesch",
    "lg" -> "Luganda",
    "li" -> "Limburgs",
    "ln" -> "Lingála",
    "lo" -> "ພາສາລາວ",
    "lt" -> "lietuvių kalba",
    "lv" -> "latviešu valoda",
    "gv" -> "Gaelg, Gailck",
    "mk" -> "македонски јази",
    "mg" -> "Malagasy fiteny",
    "ml" -> "മലയാ",
    "mt" -> "Malt",
    "mi" -> "te reo Māor",
    "mr" -> "मरा",
    "mh" -> "Kajin M̧ajeļ",
    "mn" -> "монгол",
    "na" -> "Ekakairũ Naoer",
    "nv" -> "Diné bizaad, Dinékʼehǰí",
    "nb" -> "Norsk bokmål",
    "nd" -> "isiNdebele",
    "ne" -> "नेपा",
    "ng" -> "Owamb",
    "nn" -> "Norsk nynorsk",
    "nr" -> "isiNdebel",
    "oc" -> "Occitan",
    "oj" -> "ᐊᓂᔑᓈᐯᒧᐎ",
    "cu" -> "ѩзыкъ словѣньскъ",
    "om" -> "Afaan Oromo",
    "or" -> "ଓଡ଼ି",
    "os" -> "ирон æвза",
    "pi" -> "पा",
    "pl" -> "polski",
    "ps" -> "پښتو",
    "pt" -> "Português",
    "qu" -> "Runa Simi, Kichw",
    "rm" -> "rumantsch grischu",
    "rn" -> "kiRund",
    "ro" -> "Română",
    "ru" -> "русский язык",
    "sa" -> "संस्कृत",
    "sc" -> "sard",
    "se" -> "Davvisámegiell",
    "sm" -> "gagana fa'a Samo",
    "sg" -> "yângâ tî säng",
    "sr" -> "српски јези",
    "gd" -> "Gàidhlig",
    "sn" -> "chiShon",
    "si" -> "සිංහ",
    "sk" -> "slovenčina",
    "sl" -> "slovenščina",
    "so" -> "Soomaaliga, af Soomaal",
    "st" -> "Sesoth",
    "es" -> "español, castellano",
    "su" -> "Basa Sunda",
    "sw" -> "Kiswahili",
    "ss" -> "siSwati",
    "sv" -> "svenska",
    "ta" -> "தமி",
    "te" -> "తెలు",
    "th" -> "ไท",
    "ti" -> "ትግር",
    "tk" -> "Türkmençe",
    "tn" -> "Setswan",
    "to" -> "faka Tonga",
    "tp" -> "toki pona",
    "tr" -> "Türkçe",
    "ts" -> "Xitsong",
    "tw" -> "Twi",
    "ty" -> "Reo Tahit",
    "uk" -> "українськ",
    "ve" -> "Tshivenḓ",
    "vi" -> "Tiếng Việt",
    "vo" -> "Volapük",
    "wa" -> "Walon",
    "cy" -> "Cymrae",
    "wo" -> "Wolof",
    "fy" -> "Frysk",
    "xh" -> "isiXhosa",
    "yo" -> "Yorùbá",
    "za" -> "Saɯ cueŋƅ, Saw cueng",
    "zu" -> "isiZulu")
}
