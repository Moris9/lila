package lila.shutup

case class UserRecord(
    _id: String,
    puf: List[Double],
    tef: List[Double],
    prm: List[Double],
    prc: List[Double],
    puc: List[Double]) {

  def userId = _id

  def reports: List[TextReport] = List(
    TextReport(TextType.PublicForumMessage, puf),
    TextReport(TextType.TeamForumMessage, tef),
    TextReport(TextType.PrivateMessage, prm),
    TextReport(TextType.PrivateChat, prc),
    TextReport(TextType.PublicChat, puc))
}

case class TextAnalysis(
    text: String,
    badWords: List[String]) {

  lazy val nbWords = text.split("""\W+""").size

  def nbBadWords = badWords.size

  def ratio: Double = if (nbWords == 0) 0 else nbBadWords.toDouble / nbWords
}

sealed abstract class TextType(
  val key: String,
  val rotation: Int,
  val name: String)

object TextType {

  case object PublicForumMessage extends TextType("puf", 20, "Public forum message")
  case object TeamForumMessage extends TextType("tef", 20, "Team forum message")
  case object PrivateMessage extends TextType("prm", 20, "Private message")
  case object PrivateChat extends TextType("prc", 50, "Private chat")
  case object PublicChat extends TextType("puc", 50, "Public chat")
}

case class TextReport(textType: TextType, ratios: List[Double]) {

  def minRatios = textType.rotation / 10
  def nbBad = ratios.count(_ > TextReport.unacceptableRatio)
  def tolerableNb = ratios.size / 10

  def unacceptable = (ratios.size >= minRatios) && (nbBad > tolerableNb)
}

object TextReport {

  val unacceptableRatio = 1d / 30
}
