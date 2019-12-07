package lila.app
package http

import akka.stream.Materializer
import play.api.Logging
import play.api.mvc._
import play.api.routing._

import lila.common.HTTPRequest

final class HttpFilter(env: Env)(implicit val mat: Materializer) extends Filter {

  private val httpMon = lila.mon.http
  private val net = env.net
  private val logger = lila.log("http")

  def apply(nextFilter: RequestHeader => Fu[Result])(req: RequestHeader): Fu[Result] = {

    val startTime = nowMillis

    redirectWrongDomain(req) map fuccess getOrElse {
      nextFilter(req) dmap addApiResponseHeaders(req) dmap { result =>
        monitorTime(req, startTime)
        result
      }
    }
  }

  private def monitorTime(req: RequestHeader, startTime: Long) = {
    val actionName = HTTPRequest actionName req
    val reqTime = nowMillis - startTime
    if (env.isDev) logger.info(s"$req $actionName ${reqTime}ms")
    httpMon.time(actionName)(reqTime)
    httpMon.request.all()
    if (req.remoteAddress contains ":") httpMon.request.ipv6()
    if (HTTPRequest isXhr req) httpMon.request.xhr()
    else if (HTTPRequest isBot req) httpMon.request.bot()
    else httpMon.request.page()
  }

  private def redirectWrongDomain(req: RequestHeader): Option[Result] = (
    req.host != net.domain.value &&
    HTTPRequest.isRedirectable(req) &&
    !HTTPRequest.isProgrammatic(req) &&
    // asset request going through the CDN, don't redirect
    !(req.host == net.assetDomain.value && HTTPRequest.hasFileExtension(req))
  ) option Results.MovedPermanently(s"http${if (req.secure) "s" else ""}://${net.domain}${req.uri}")

  private def addApiResponseHeaders(req: RequestHeader)(result: Result) =
    if (HTTPRequest.isApiOrApp(req))
      result.withHeaders(ResponseHeaders.headersForApiOrApp(req): _*)
    else
      result
}
