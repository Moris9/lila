package lila.api

import play.api.libs.json._

import chess.format.pgn.Pgn
import lila.analyse.{ AnalysisRepo, Analysis }
import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.game.Game.{ BSONFields => G }
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.hub.actorApi.{ router => R }
import makeTimeout.short

private[api] final class GameApi(
    netBaseUrl: String,
    apiToken: String,
    pgnDump: PgnDump,
    analysisApi: AnalysisApi) {

  def list(
    username: Option[String],
    rated: Option[Boolean],
    analysed: Option[Boolean],
    withAnalysis: Boolean,
    withMoves: Boolean,
    withOpening: Boolean,
    token: Option[String],
    nb: Option[Int]): Fu[JsObject] = $find($query(Json.obj(
    G.status -> $gte(chess.Status.Mate.id),
    G.playerUids -> username.map(_.toLowerCase),
    G.rated -> rated.map(_.fold(JsBoolean(true), $exists(false))),
    G.analysed -> analysed.map(_.fold(JsBoolean(true), $exists(false))),
    G.variant -> check(token).option($nin(Game.unanalysableVariants.map(_.id)))
  ).noNull) sort lila.game.Query.sortCreated, math.min(200, nb | 10)) flatMap
    gamesJson(
      withAnalysis = withAnalysis,
      withMoves = withMoves,
      withOpening = withOpening,
      withFens = false,
      token = token) map { games =>
        Json.obj("list" -> games)
      }

  def one(
    id: String,
    withAnalysis: Boolean,
    withMoves: Boolean,
    withOpening: Boolean,
    withFens: Boolean,
    token: Option[String]): Fu[Option[JsObject]] =
    $find byId id flatMap {
      _ ?? { g =>
        gamesJson(
          withAnalysis = withAnalysis,
          withMoves = withMoves,
          withOpening = withOpening,
          withFens = withFens && g.finished,
          token = token
        )(List(g)) map (_.headOption)
      }
    }

  private def makeUrl(game: Game) = s"$netBaseUrl/${game.id}/${game.firstPlayer.color.name}"

  private def gamesJson(
    withAnalysis: Boolean,
    withMoves: Boolean,
    withOpening: Boolean,
    withFens: Boolean,
    token: Option[String])(games: List[Game]): Fu[List[JsObject]] =
    AnalysisRepo doneByIds games.map(_.id) flatMap { analysisOptions =>
      (games map GameRepo.initialFen).sequenceFu map { initialFens =>
        val validToken = check(token)
        games zip analysisOptions zip initialFens map {
          case ((g, analysisOption), initialFen) =>
            val pgnOption = withAnalysis option pgnDump(g, initialFen)
            gameToJson(g, makeUrl(g), analysisOption, pgnOption, initialFen,
              withAnalysis = withAnalysis,
              withMoves = withMoves,
              withOpening = withOpening,
              withFens = withFens,
              withBlurs = validToken,
              withHold = validToken,
              withMoveTimes = validToken)
        }
      }
    }

  private def check(token: Option[String]) = token ?? (apiToken==)

  private def gameToJson(
    g: Game,
    url: String,
    analysisOption: Option[Analysis],
    pgnOption: Option[Pgn],
    initialFen: Option[String],
    withAnalysis: Boolean,
    withMoves: Boolean,
    withOpening: Boolean,
    withFens: Boolean,
    withBlurs: Boolean = false,
    withHold: Boolean = false,
    withMoveTimes: Boolean = false) = Json.obj(
    "id" -> g.id,
    "initialFen" -> initialFen,
    "rated" -> g.rated,
    "variant" -> g.variant.key,
    "speed" -> g.speed.key,
    "perf" -> PerfPicker.key(g),
    "timestamp" -> g.createdAt.getDate,
    "turns" -> g.turns,
    "status" -> g.status.name,
    "clock" -> g.clock.map { clock =>
      Json.obj(
        "initial" -> clock.limit,
        "increment" -> clock.increment,
        "totalTime" -> clock.estimateTotalTime
      )
    },
    "players" -> JsObject(g.players.zipWithIndex map {
      case (p, i) => p.color.name -> Json.obj(
        "userId" -> p.userId,
        "name" -> p.name,
        "rating" -> p.rating,
        "ratingDiff" -> p.ratingDiff,
        "provisional" -> p.provisional.option(true),
        "moveTimes" -> withMoveTimes.fold(
          g.moveTimes.zipWithIndex.filter(_._2 % 2 == i).map(_._1),
          JsNull),
        "blurs" -> withBlurs.option(p.blurs),
        "hold" -> p.holdAlert.ifTrue(withHold).fold[JsValue](JsNull) { h =>
          Json.obj(
            "ply" -> h.ply,
            "mean" -> h.mean,
            "sd" -> h.sd
          )
        },
        "analysis" -> analysisOption.flatMap(analysisApi.player(p.color))
      ).noNull
    }),
    "analysis" -> analysisOption.ifTrue(withAnalysis).|@|(pgnOption).apply(analysisApi.game),
    "moves" -> withMoves.option(g.pgnMoves mkString " "),
    "opening" -> withOpening.?? {
      g.opening map { opening =>
        Json.obj("code" -> opening.code, "name" -> opening.name)
      }
    },
    "fens" -> withFens ?? {
      chess.Replay.boards(g.pgnMoves, initialFen, g.variant).toOption map { boards =>
        JsArray(boards map chess.format.Forsyth.exportBoard map JsString.apply)
      }
    },
    "winner" -> g.winnerColor.map(_.name),
    "url" -> url
  ).noNull
}
