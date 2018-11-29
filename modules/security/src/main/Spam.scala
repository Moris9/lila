package lila.security

final class Spam(
    spamKeywords: () => lila.common.Strings,
    bannedYoutubeIds: List[String]
) {

  def detect(text: String) = staticBlacklist.exists(text.contains) ||
    spamKeywords().value.exists(text.contains)

  private def referBlacklist = List(
    /* While links to other chess websites are welcome,
     * refer links grant the referrer money,
     * effectively inducing spam */
    "chess24.com?ref=",
    "chess.com/register?refId="
  )

  private lazy val staticBlacklist = List("chess-bot.com") ::: bannedYoutubeIds ::: referBlacklist

  def replace(text: String) = replacements.foldLeft(text) {
    case (t, (regex, rep)) => regex.replaceAllIn(t, rep)
  }

  private val protocol = """https?://"""

  private val replacements = List(
    """chess24.com\?ref=\w+""".r -> "chess24.com",
    """chess.com/register\?refId=\w+""".r -> "chess.com",
    """\bchess-bot(\.com)?[^\s]*""".r -> "[redacted]"
  ) ::: bannedYoutubeIds.map { id =>
      id.r -> "7orFjhLkcxA"
    }
}
