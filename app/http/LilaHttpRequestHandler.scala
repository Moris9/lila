package lila.app
package http

import play.api.http.{ DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpRequestHandler }
import play.api.mvc.{ ControllerComponents, EssentialFilter, Handler, RequestHeader, Results }
import play.api.routing.Router

import lila.common.Chronometer

final class LilaHttpRequestHandler(
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: Seq[EssentialFilter],
    controllerComponents: ControllerComponents
) extends DefaultHttpRequestHandler(() => router, errorHandler, configuration, filters) {

  private val monitorPaths = Set("/tv", "/robots.txt")

  override def routeRequest(request: RequestHeader): Option[Handler] =
    if (monitorPaths(request.path))
      Chronometer.syncMon(_.http.router(request.path)) {
        router handlerFor request
      }
    else if (request.method == "OPTIONS") optionsHandler.some
    else router handlerFor request

  // should be handled by nginx in production
  private val optionsHandler =
    controllerComponents.actionBuilder { req =>
      if (lila.common.HTTPRequest.isApiOrApp(req))
        Results.NoContent.withHeaders(
          "Allow"                  -> ResponseHeaders.allowMethods,
          "Access-Control-Max-Age" -> "86400"
        )
      else Results.NotFound
    }
}
