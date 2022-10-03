package lila.opening

import chess.format.FEN
import com.softwaremill.tagging._
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.ExecutionContext
import chess.format.Forsyth
import lila.game.Game

final private class OpeningExplorer(
    ws: StandaloneWSClient,
    explorerEndpoint: String @@ ExplorerEndpoint
)(implicit ec: ExecutionContext) {
  import OpeningExplorer._

  def stats(query: OpeningQuery): Fu[Option[Position]] =
    ws.url(s"$explorerEndpoint/lichess")
      .withQueryStringParameters(
        queryParameters(query) ::: List(
          "moves" -> "12"
        ): _*
      )
      .get()
      .flatMap {
        case res if res.status == 404 => fuccess(none)
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer: ${res.status} for $query")
        case res =>
          res
            .body[JsValue]
            .validate[Position](positionReads)
            .fold(
              err => fufail(s"Couldn't parse opening data for $query: $err"),
              data => fuccess(data.some)
            )
      }

  def queryHistory(query: OpeningQuery): Fu[PopularityHistoryAbsolute] =
    historyOf(queryParameters(query))

  def configHistory(config: OpeningConfig): Fu[PopularityHistoryAbsolute] =
    historyOf(configParameters(config))

  private def historyOf(params: List[(String, String)]): Fu[PopularityHistoryAbsolute] =
    ws.url(s"$explorerEndpoint/lichess/history")
      .withQueryStringParameters(params ::: List("since" -> OpeningQuery.firstMonth): _*)
      .get()
      .flatMap {
        case res if res.status != 200 =>
          fufail(s"Opening explorer: ${res.status}")
        case res =>
          (res.body[JsValue] \ "history")
            .validate[List[JsObject]]
            .fold(
              invalid => fufail(invalid.toString),
              months =>
                fuccess {
                  months.map { o =>
                    ~o.int("black") + ~o.int("draws") + ~o.int("white")
                  }
                }
            )
      }
      .recover { case e: Exception =>
        logger.warn(s"Opening history $params", e)
        Nil
      }

  private def queryParameters(query: OpeningQuery) =
    configParameters(query.config) ::: List(
      "play" -> query.uci.map(_.uci).mkString(",")
    )
  private def configParameters(config: OpeningConfig) =
    List(
      "ratings" -> config.ratings.mkString(","),
      "speeds"  -> config.speeds.map(_.key).mkString(",")
    )
}

object OpeningExplorer {

  case class Position(
      white: Int,
      draws: Int,
      black: Int,
      moves: List[Move],
      topGames: List[GameRef],
      recentGames: List[GameRef]
  ) {
    val movesSum = moves.foldLeft(0L)(_ + _.sum)
    val games    = topGames ::: recentGames
  }

  case class Move(uci: String, san: String, averageRating: Int, white: Int, draws: Int, black: Int) {
    def sum = white + draws + black
  }

  case class GameRef(id: Game.ID)

  implicit private val moveReads     = Json.reads[Move]
  implicit private val gameReads     = Json.reads[GameRef]
  implicit private val positionReads = Json.reads[Position]
}
