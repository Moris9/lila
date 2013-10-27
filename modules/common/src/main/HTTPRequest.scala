package lila.common

import play.api.mvc.RequestHeader
import play.api.http.HeaderNames

object HTTPRequest {

  def isXhr(req: RequestHeader): Boolean = 
    (req.headers get "X-Requested-With") == Some("XMLHttpRequest")

  def isSocket(req: RequestHeader): Boolean = 
    (req.headers get HeaderNames.UPGRADE) == Some("websocket")

  def isSynchronousHttp(req: RequestHeader) = !isXhr(req) && !isSocket(req)

  def fullUrl(req: RequestHeader): String = 
    "http://" + req.host + req.uri

  def userAgent(req: RequestHeader): Option[String] = 
    req.headers get HeaderNames.USER_AGENT

  def sid(req: RequestHeader): Option[String] =
    req.session get "sid"
}
