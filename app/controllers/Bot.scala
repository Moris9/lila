package controllers

import org.joda.time.DateTime
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._

object Bot extends LilaController {

  def gameStream(id: String) = Scoped(_.Bot.Play) { req => me =>
    WithMyBotGame(id, me) { pov =>
      RequireHttp11(req) {
        lila.game.GameRepo.withInitialFen(pov.game) map { wf =>
          Ok.chunked(Env.bot.gameStateStream(me, wf, pov.color))
        }
      }
    }
  }

  def move(id: String, uci: String) = Scoped(_.Bot.Play) { _ => me =>
    WithMyBotGame(id, me) { pov =>
      Env.bot.player(pov, uci) inject jsonOkResult recover {
        case e: Exception => BadRequest(jsonError(e.getMessage))
      }
    }
  }

  def command(cmd: String) = Scoped(_.Bot.Play) { _ => me =>
    cmd.split('/') match {
      case Array("account", "upgrade") =>
        lila.user.UserRepo.setBot(me) >>- Env.user.lightUserApi.invalidate(me.id) inject jsonOkResult recover {
          case e: Exception => BadRequest(jsonError(e.getMessage))
        }
      case _ => notFoundJson("No such command")
    }
  }

  private def WithMyBotGame(anyId: String, me: lila.user.User)(f: lila.game.Pov => Fu[Result]) =
    if (!me.isBot) BadRequest(jsonError("This endpoint only works for bot accounts. See https://lichess.org/api#operation/botAccountUpgrade")).fuccess
    else Env.round.roundProxyGame(lila.game.Game takeGameId anyId) flatMap {
      case None => NotFound(jsonError("No such game")).fuccess
      case Some(game) => lila.game.Pov(game, me) match {
        case None => NotFound(jsonError("Not your game")).fuccess
        case Some(pov) => f(pov)
      }
    }
}
