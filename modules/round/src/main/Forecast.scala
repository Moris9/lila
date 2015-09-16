package lila.round

import org.joda.time.DateTime
import play.api.libs.json._

import chess.Pos
import chess.format.UciMove
import lila.game.Game

case class Forecast(
    _id: String, // player full id
    ply: Int,
    steps: Forecast.Steps,
    date: DateTime) {

  def apply(g: Game): (Forecast, Option[UciMove]) = (this, none)
}

object Forecast {

  type Steps = List[List[Step]]

  case class Step(
    ply: Int,
    uci: String,
    san: String,
    fen: String,
    check: Option[Boolean],
    dests: String)

  implicit val forecastStepJsonFormat = Json.format[Step]

  implicit val forecastJsonWriter = Json.writes[Forecast]

  case object OutOfSync extends lila.common.LilaException {
    val message = "Forecast out of sync"
  }
}
