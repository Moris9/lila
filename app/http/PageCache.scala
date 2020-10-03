package lila.app
package http

import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context

final class PageCache(cacheApi: lila.memo.CacheApi) {

  private val cache = cacheApi.notLoading[String, Result](16, "pageCache") {
    _.expireAfterWrite(1.seconds).buildAsync()
  }

  def apply(key: String)(compute: () => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.isAnon && langs(ctx.lang.language) && defaultPrefs(ctx.req) && !hasCookies(ctx.req))
      cache.getFuture(cacheKey(key), _ => compute())
    else
      compute()

  private def cacheKey(key: String)(implicit ctx: Context) =
    s"$key(${ctx.lang.language})"

  private def defaultPrefs(req: RequestHeader) =
    lila.pref.RequestPref.fromRequest(req) == lila.pref.Pref.default

  private val langs =
    Set("en", "ru", "tr", "de", "es", "fr", "pt", "it", "pl", "ar", "fa", "id", "nl", "nb", "sv")

  private def hasCookies(req: RequestHeader) =
    lila.security.EmailConfirm.cookie.has(req)
}
