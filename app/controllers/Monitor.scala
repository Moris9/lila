package controllers

import play.api.mvc._
import play.api.libs.Comet
import play.api.libs.concurrent._
import play.api.libs.json._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout

import lila._
import socket.GetNbMembers
import monitor._

object Monitor extends LilaController {

  private def reporting = env.monitor.reporting
  private def usernameMemo = env.user.usernameMemo
  private implicit def timeout = Timeout(500 millis)

  def index = Action {
    Ok(views.html.monitor.monitor())
  }

  def websocket = WebSocket.async[JsValue] { implicit req ⇒
    env.monitor.socket.join(uidOption = get("sri", req))
  }

  def status = Action {
    Async {
      (reporting ? GetStatus).mapTo[String] map { Ok(_) }
    }
  }

  def nbPlayers = Action {
    Async {
      (reporting ? GetNbMembers).mapTo[Int] map { players ⇒
        Ok("%d %d".format(players, usernameMemo.preciseCount))
      }
    }
  }

  def nbMoves = Action {
    Async {
      (reporting ? GetNbMoves).mapTo[Int] map { Ok(_) }
    }
  }
}
