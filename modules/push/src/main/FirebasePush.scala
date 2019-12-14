package lila.push

import com.google.auth.oauth2.{ AccessToken, GoogleCredentials }
import io.methvin.play.autoconfig._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import scala.concurrent.Future
import scala.concurrent.duration._

import lila.common.WorkQueue
import lila.user.User

final private class FirebasePush(
    credentialsOpt: Option[GoogleCredentials],
    deviceApi: DeviceApi,
    ws: WSClient,
    config: OneSignalPush.Config
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  private val workQueue = new WorkQueue(512, "firebasePush")

  def apply(userId: User.ID)(data: => PushApi.Data): Funit =
    credentialsOpt ?? { creds =>
      deviceApi.findLastManyByUserId("firebase", 3)(userId) flatMap {
        case Nil => funit
        // access token has 1h lifetime and is requested only if expired
        case devices =>
          workQueue {
            Future {
              creds.refreshIfExpired()
              creds.getAccessToken()
            } withTimeout 10.seconds
          }.chronometer.mon(_.push.googleTokenTime).result flatMap { token =>
            // TODO http batch request is possible using a multipart/mixed content
            // unfortuntely it doesn't seem easily doable with play WS
            devices.map(send(token, _, data)).sequenceFu.void
          }
      }
    }

  private def send(token: AccessToken, device: Device, data: => PushApi.Data): Funit =
    ws.url(config.url)
      .withHttpHeaders(
        "Authorization" -> s"Bearer ${token.getTokenValue}",
        "Accept"        -> "application/json",
        "Content-type"  -> "application/json; UTF-8"
      )
      .post(
        Json.obj(
          "message" -> Json.obj(
            "token" -> device._id,
            // firebase doesn't support nested data object and we only use what is
            // inside userData
            "data" -> (data.payload \ "userData").asOpt[JsObject].map(transform(_)),
            "notification" -> Json.obj(
              "body"  -> data.body,
              "title" -> data.title
            )
          )
        )
      ) flatMap {
      case res if res.status == 200 => funit
      case res                      => fufail(s"[push] firebase: ${res.status} ${res.body}")
    }

  // filter out any non string value, otherwise Firebase API silently rejects
  // the request
  private def transform(obj: JsObject): JsObject =
    JsObject(obj.fields.collect {
      case (k, v: JsString) => s"lichess.$k" -> v
      case (k, v: JsNumber) => s"lichess.$k" -> JsString(v.toString)
    })
}

private object FirebasePush {

  final class Config(
      val url: String,
      val json: lila.common.config.Secret
  )
  implicit val configLoader = AutoConfig.loader[Config]
}
