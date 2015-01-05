package lila.i18n

object LangList {

  def name(code: String) = all get code

  def nameOrCode(code: String) = name(code) | code

  def exists(code: String) = all contains code

  lazy val sortedList = all.toList sortBy (_._1)

  val all = Map(
    "ab" -> "аҧсуа",
    "aa" -> "ʿAfár af",
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
    "ay" -> "Aymar aru",
    "az" -> "Azərbaycanca",
    "bm" -> "bamanankan",
    "ba" -> "Башҡортса",
    "eu" -> "Euskara",
    "be" -> "Беларуская",
    "bn" -> "বাংলা",
    "bh" -> "भोजपुरी",
    "bi" -> "Bislama",
    "bs" -> "bosanski jezik",
    "br" -> "brezhoneg",
    "bg" -> "български език",
    "ca" -> "Català, valencià",
    "ch" -> "Chamoru",
    "ce" -> "нохчийн мотт",
    "ny" -> "chiCheŵa, chinyanja",
    "zh" -> "中文",
    "cv" -> "чӑваш чӗлхи",
    "kw" -> "Kernewek",
    "co" -> "corsu, lingua corsa",
    "cr" -> "ᓀᐦᐃᔭᐍᐏᐣ",
    "hr" -> "hrvatski",
    "cs" -> "čeština",
    "da" -> "Dansk",
    "nl" -> "Nederlands",
    "en" -> "English",
    "eo" -> "Esperanto",
    "et" -> "eesti keel",
    "ee" -> "Eʋegbe",
    "fa" -> "فارسی",
    "fo" -> "føroyskt",
    "fj" -> "vosa Vakaviti",
    "fi" -> "suomen kieli",
    "ff" -> "Filipino",
    "fr" -> "français",
    "fp" -> "arpitan",
    //"frp" -> "arpitan",
    "ff" -> "Fulfulde, Pulaar, Pular",
    "gl" -> "Galego",
    "ka" -> "ქართულ",
    "kb" -> "Taqvaylit", // should be kab http://en.wikipedia.org/wiki/Kabyle_language
    "de" -> "Deutsch",
    "el" -> "Ελληνικά",
    "gn" -> "avañe'ẽ",
    "gu" -> "ગુજરાતી",
    "he" -> "עִבְרִית",
    "ht" -> "kreyòl ayisyen",
    "hz" -> "Otjiherero",
    "hi" -> "हिन्दी, हिंदी",
    "ho" -> "Hiri Motu",
    "hu" -> "Magyar",
    "ia" -> "Interlingua",
    "ga" -> "Gaeilge",
    "id" -> "Bahasa Indonesia",
    "ie" -> "Interlingue",
    "ig" -> "Asụsụ Igbo",
    "ik" -> "Iñupiaq, Iñupiatun", // lots of different writing systems (and therefore spellings) for 'ik' -- the Inupiat language
    "io" -> "Ido",
    "is" -> "Íslenska",
    "it" -> "Italiano",
    "iu" -> "ᐃᓄᒃᑎᑐ",
    "ja" -> "日本語",
    "jb" -> "lojban",
    "jv" -> "basa Jawa",
    "kl" -> "kalaallisut",
    "kn" -> "ಕನ್ನಡ",
    "kr" -> "Kanuri",
    "kk" -> "Қазақ тілі",
    "km" -> "ភាសាខ្មែរ",
    "ki" -> "Gĩkũy",
    "rw" -> "Kinyarwanda",
    "ky" -> "кыргыз тили",
    "kv" -> "коми кыв",
    "kg" -> "Kikongo, Kitubà",
    "kj" -> "Kuanyam",
    "ko" -> "한국어",
    "lb" -> "Lëtzebuergesch",
    "lg" -> "Oluganda",
    "la" -> "lingua Latīna",
    "le" -> "1337", // not in the ISO 639 standards (duh!)
    "li" -> "Lèmbörgs",
    "ln" -> "Lingála",
    "lo" -> "ພາສາລາວ",
    "lt" -> "lietuvių kalba",
    "lv" -> "latviešu valoda",
    "gv" -> "Gaelg, Gailck",
    "mk" -> "македонски јази",
    "mg" -> "fiteny malagasy",
    "ml" -> "മലയാ",
    "mt" -> "Malti",
    "mi" -> "reo Māori",
    "mr" -> "मराठी",
    "mh" -> "Kajin M̧ajeļ",
    "mn" -> "монгол",
    "na" -> "Ekakairũ Naoero",
    "nv" -> "Diné bizaad",
    "nb" -> "Norsk bokmål",
    "nd" -> "isiNdebele",
    "ne" -> "नेपा",
    "ng" -> "Oshiwambo",
    "nn" -> "Norsk nynorsk",
    "nr" -> "isiNdebele",
    "oc" -> "Occitan",
    "oj" -> "Ojibwe", // difficult to find a native name for this language group. Also, the spelling is nonstandard but this seems to be the most prominent in recent source
    "cu" -> "словѣ́ньскъ ѩзꙑ́къ",
    "om" -> "Afaan Oromo",
    "or" -> "ଓଡ଼ି",
    "os" -> "Ирон",
    "pi" -> "पालि",
    "pl" -> "polski",
    "ps" -> "پښتو",
    "pt" -> "Português",
    "qu" -> "Runa Simi, Kichwa",
    "rm" -> "Rumantsch Grischun",
    "rn" -> "kiRund",
    "ro" -> "Română",
    "ru" -> "русский язык",
    "sa" -> "संस्कृत",
    "sc" -> "sardu, saldu",
    "se" -> "Davvisámegiella",
    "sm" -> "gagana Sāmoa",
    "sg" -> "yângâ tî sängö",
    "sr" -> "Српски језик",
    "gd" -> "Gàidhlig",
    "sn" -> "chiShona",
    "si" -> "සිංහල",
    "sk" -> "slovenčina",
    "sl" -> "slovenščina",
    "so" -> "af-Soomaali",
    "st" -> "Sesotho",
    "es" -> "español, castellano",
    "su" -> "Basa Sunda",
    "sw" -> "Kiswahili",
    "ss" -> "siSwati",
    "sv" -> "svenska",
    "ta" -> "தமிழ்",
    "te" -> "తెలుగు",
    "th" -> "ไทย",
    "ti" -> "ትግርኛ",
    "tk" -> "Türkmençe",
    "tl" -> "tlhIngan Hol",
    //"tlh" -> "tlhIngan Hol"
    "tn" -> "Setswana",
    "to" -> "lea fakatonga", // a variety of different spellings for this
    "tp" -> "toki pona", // note that this constructed language has no ISO code
    "tr" -> "Türkçe",
    "ts" -> "Xitsonga",
    "tw" -> "Twi",
    //"twi" -> "Twi",
    "ty" -> "Reo Mā`ohi, Reo Tahiti",
    "uk" -> "українська",
    "ur" -> "اُردُو",
    "ug" -> "ئۇيغۇرچە",
    "ve" -> "Tshivenḓa",
    "vi" -> "Tiếng Việt",
    "vo" -> "Volapük",
    "wa" -> "Walon",
    "cy" -> "Cymrae",
    "wo" -> "Wolof",
    "fy" -> "Frysk",
    "xh" -> "isiXhosa",
    "yo" -> "Yorùbá",
    "za" -> "Saɯ cueŋƅ, Saw cueng",
    // the values for 'za' (Zhang languages) are made up. Contact with a native speaker to determine their native name is necessary (won't be hard; it has many speakers)
    "zu" -> "isiZulu")
}
