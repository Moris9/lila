package lila.analyse

import play.api.libs.json.Json

private[analyse] final class AdvantageChart(advices: InfoAdvices) {

  val max = 15

  def columns = Json.arr(
    Json.arr("string", "Move"),
    Json.arr("number", "Advantage")
  )

  def rows = Json toJson {

    val scale = floatBox(-max to max) _

    def move(info: Info, advice: Option[Advice]) = info.color.fold(
      "%d. %s", "%d... %s"
    ).format(info.turn, info.move.uci) + ~advice.map(" " + _.nag.symbol)

    advices sliding 2 collect {
      case (info, advice) :: (next, _) :: Nil ⇒
        (next.score, next.mate) match {
          case (Some(score), _) ⇒ Json.arr(move(info, advice), scale(score.pawns).toString)
          case (_, Some(mate)) ⇒ Json.arr(move(info, advice), {
            val mateDelta = math.abs(mate.toFloat / 100)
            val whiteWins = info.color.fold(mate < 0, mate > 0)
            scale(whiteWins.fold(max - mateDelta, mateDelta - max))
          })
          case _ ⇒ Json.arr(move(info, none), scale(0))
        }
    } toList
  } 
}
