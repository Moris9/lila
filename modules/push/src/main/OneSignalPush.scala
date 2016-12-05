package lila.push

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private final class OneSignalPush(
    getDevices: String => Fu[List[Device]],
    url: String,
    appId: String,
    key: String) {

  def apply(userId: String)(data: => PushApi.Data): Funit =
    getDevices(userId) flatMap {
      case Nil => funit
      case devices =>
        WS.url(url)
          .withHeaders(
            "Authorization" -> s"key=$key",
            "Accept" -> "application/json",
            "Content-type" -> "application/json")
          .post(Json.obj(
            "app_id" -> appId,
            "include_player_ids" -> devices.map(_.deviceId),
            "headings" -> Map("en" -> data.title),
            "contents" -> Map("en" -> data.body),
            "data" -> data.payload,
            "android_group" -> data.group,
            "collapse_id" -> data.group
          )).flatMap {
            case res if res.status == 200 =>
              (res.json \ "errors").asOpt[List[String]] match {
                case Some(errors) =>
                  println(errors mkString ",")
                  fufail(s"[push] ${devices.map(_.deviceId)} $data ${res.status} ${res.body}")
                case None => funit
              }
            case res => fufail(s"[push] ${devices.map(_.deviceId)} $data ${res.status} ${res.body}")
          }
    }
}
