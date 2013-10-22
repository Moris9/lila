package lila.socket

import play.api.libs.json._

private[socket] trait Socket {

  def makeMessage[A: Writes](t: String, data: A): JsObject =
    Json.obj("t" -> t, "d" -> data)

  def makeMessage(t: String): JsObject = Json.obj("t" -> t)
}
