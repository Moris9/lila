package lila.shutup

// from https://github.com/snipe/banbuilder/tree/master/src/dict
object Dictionary {

  val en: List[String] = (enBase ++ enUk ++ enUs).distinct

  private def enBase = List("anal", "anus", "ass", "bastard", "bitch", "boob", "cock", "cum", "cunnilingu", "cunt", "cunting", "dick",
    "dildo", "dyke", "fag", "faggot", "fuck", "fuk", "fack", "fuckstick", "fucktard", "fucking", "handjob", "homo", "jerk", "jizz", "kike",
    "kunt", "muff", "nigger", "niger", "pederast", "penis", "piss", "poop", "pussy", "queer", "rape", "scum", "scumbag", "semen", "sex", "shit",
    "shite", "shitty", "shity", "shitbag", "slut", "titties", "twat", "vagin", "vagina", "vulva", "wank")
  private def enUk = List("analplug", "analsex", "arse", "assassin", "balls", "bimbo", "bloody", "bloodyhell", "blowjob", "bollocks",
    "boner", "boobies", "boobs", "bugger", "bukkake", "bullshit", "chink", "clit", "clitoris", "cocksucker", "condom", "coon", "crap",
    "cumshot", "damm", "dammit", "damn", "dickhead", "doggystyle", "f0ck", "fags", "fanny", "fck", "fcker", "fckr", "fcku", "fcuk",
    "fucked", "fucker", "fuckface", "fuckr", "fuct", "genital", "genitalia", "genitals", "glory hole", "gloryhole", "gobshite", "godammet", "godammit", "goddammet", "goddammit", "goddamn", "gypo", "hitler", "hooker", "hore", "horny", "jesussucks", "jizzum", "kaffir", "kill", "killer", "killin", "killing", "lesbo", "masturbate", "milf", "molest", "moron", "motherfuck", "mthrfckr", "murder", "murderer", "nazi", "negro", "nigga", "niggah", "nonce", "paedo", "paedophile", "paki", "pecker", "pedo", "pedofile", "pedophile", "phuk", "pig", "pimp", "poof", "porn", "prick", "pron", "prostitute", "raped", "rapes", "rapist", "schlong", "screw", "scrotum", "shag", "shemale", "shite", "shiz", "slag", "spastic", "spaz", "sperm", "spunk", "stripper", "tart", "terrorist", "tits", "tittyfuck", "tosser", "turd", "vaginal", "vibrator", "wanker", "weed", "wetback", "whor", "whore", "wog", "wtf", "xxx")
  private def enUs = List("abortion", "anus", "beastiality", "bestiality", "bewb", "blow", "blumpkin", "cawk", "choad", "cooter", "cornhole", "dong", "douche", "fart", "foreskin", "gangbang", "gook", "hell", "honkey", "humping", "jiz", "labia", "nutsack", "pen1s", "poon", "punani", "queef", "quim", "rectal", "rectum", "rimjob", "spick", "spoo", "spooge", "taint", "titty", "vag", "whore")
}
