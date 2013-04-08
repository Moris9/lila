package lila.socket

import play.api.libs.json._

trait Historical[M <: Member] { self: SocketActor[M] ⇒

  val history: History

  def notifyVersion(t: String, data: JsValue) {
    val vmsg = history += makeMessage(t, data)
    members.values.foreach(_.channel push vmsg)
  }
  def notifyVersion(t: String, data: Seq[(String, JsValue)]) {
    notifyVersion(t, JsObject(data))
  }
}
