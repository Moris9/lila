package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._

import play.api.libs.ws.WS
import play.api.Play.current

object Ai extends LilaController {

  def move = Action.async { req =>
    Env.ai.server.move(
      uciMoves = get("uciMoves", req) ?? (_.split(' ').toList),
      initialFen = get("initialFen", req),
      level = getInt("level", req) | 1,
      kingOfTheHill = getBool("kingOfTheHill", req)
    ) fold (
        err => {
          logwarn("[ai] stockfish server play: " + err)
          InternalServerError("AI server failed: " + err.toString)
        },
        res => Ok(res.move)
      )
  }

  def analyse = Action.async { req =>
    get("replyUrl", req) foreach { replyToUrl =>
      println(s"analyse gameId ${get("gameId", req)}")
      Env.ai.server.analyse(
        uciMoves = get("uciMoves", req) ?? (_.split(' ').toList),
        initialFen = get("initialFen", req),
        requestedByHuman = getBool("human", req),
        kingOfTheHill = getBool("kingOfTheHill", req)
      ).effectFold(
          err => WS.url(replyToUrl).post(err.toString),
          infos => WS.url(replyToUrl).post(lila.analyse.Info encodeList infos)
        )
    }
    funit
  }

}
