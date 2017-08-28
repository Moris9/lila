package lila.common

object LameName {

  def username(name: String) =
    anyName(name) || lameTitlePrefix.matcher(name).lookingAt

  def anyName(name: String) = lameWords.matcher(name).find

  private val lameTitlePrefix =
    "[Ww]?[NCFIGl1L]M|(?i:w?[ncfigl1])m[-_A-Z0-9]".r.pattern

  private val lameWords = {
    val extras = Map(
      'a' -> "4",
      'e' -> "3",
      'i' -> "l1",
      'l' -> "I1",
      'o' -> "0",
      's' -> "5",
      'z' -> "2"
    )

    val subs = 'a' to 'z' map {
      c => c -> s"[$c${c.toUpper}${~extras.get(c)}]"
    } toMap

    (List(
      "hitler",
      "fuck",
      "penis",
      "vagin",
      "anus",
      "bastard",
      "bitch",
      "shit",
      "cunniling",
      "cunt",
      "kunt",
      "douche",
      "faggot",
      "jerk",
      "nigg",
      "coon",
      "piss",
      "poon",
      "prick",
      "pussy",
      "slut",
      "whore",
      "nazi",
      "buttsex",
      "retard",
      "pedo",
      "lichess",
      "moderator",
      "cheat",
      "administrator",
      "cock",
      "fart"
    ) map { _ map subs mkString } mkString "|" r).pattern
  }
}
