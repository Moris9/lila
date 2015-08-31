package controllers

import akka.pattern.ask
import play.api.libs.json._
import play.api.mvc._

import lila.app._
import lila.monitor.actorApi._
import lila.socket.actorApi.PopulationGet
import makeTimeout.short

object Monitor extends LilaController {

  private def env = Env.monitor

  def index = Secure(_.Admin) { ctx =>
    me =>
      Ok(views.html.monitor.monitor()).fuccess
  }

  def websocket = SocketOption[JsValue] { implicit ctx =>
    get("sri") ?? { sri =>
      env.socketHandler(sri) map some
    }
  }

  def status = Action.async { implicit req =>
    (~get("key", req) match {
      case "moves" => (env.reporting ? GetNbMoves).mapTo[Int] map { Ok(_) }
      case "players" => {
        (env.reporting ? PopulationGet).mapTo[Int] map { "%d %d".format(_, Env.user.onlineUserIdMemo.count) }
      } map { Ok(_) }
      case "uptime" => fuccess {
        val up = lila.common.PlayApp.uptime
        Ok {
          val human = org.joda.time.format.PeriodFormat.wordBased(new java.util.Locale("en")).print(up)
          s"last deploy: ${lila.common.PlayApp.startedAt}\nuptime seconds: ${up.toStandardSeconds.getSeconds}\nuptime: $human"
        }
      }
      case key => fuccess {
        BadRequest(s"Unknown monitor status key: $key")
      }
    })
  }
}
