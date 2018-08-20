package lila.app
package templating

import controllers.routes
import play.api.mvc.RequestHeader
import play.twirl.api.Html

import lila.api.Context
import lila.common.{ AssetVersion, ContentSecurityPolicy }

trait AssetHelper { self: I18nHelper =>

  def isProd: Boolean

  val assetDomain = lila.api.Env.current.Net.AssetDomain
  val socketDomain = lila.api.Env.current.Net.SocketDomain

  val assetBaseUrl = s"//$assetDomain"

  def assetUrl(path: String, version: AssetVersion): String =
    s"$assetBaseUrl/assets/$version/$path"
  def assetUrl(path: String)(implicit ctx: Context): String =
    assetUrl(path, ctx.pageData.assetVersion)

  def cdnUrl(path: String) = s"$assetBaseUrl$path"
  def staticUrl(path: String) = s"$assetBaseUrl/assets/$path"

  def dbImageUrl(path: String) = s"$assetBaseUrl/image/$path"

  def cssTag(name: String)(implicit ctx: Context): Html =
    cssAt("stylesheets/" + name)

  def cssVendorTag(name: String)(implicit ctx: Context) =
    cssAt("vendor/" + name)

  def cssAt(path: String, version: AssetVersion): Html = Html {
    val href = assetUrl(path, version)
    s"""<link href="$href" type="text/css" rel="stylesheet"/>"""
  }
  def cssAt(path: String)(implicit ctx: Context): Html =
    cssAt(path, ctx.pageData.assetVersion)

  def jsTag(name: String, async: Boolean = false)(implicit ctx: Context) =
    jsAt("javascripts/" + name, async = async)

  def jsTagCompiled(name: String)(implicit ctx: Context) =
    if (isProd) jsAt("compiled/" + name) else jsTag(name)

  def jsAt(path: String, async: Boolean, version: AssetVersion): Html = Html {
    val src = assetUrl(path, version)
    s"""<script${if (async) " async defer" else ""} src="$src"></script>"""
  }
  def jsAt(path: String, async: Boolean = false)(implicit ctx: Context): Html =
    jsAt(path, async, ctx.pageData.assetVersion)

  val jQueryTag = Html {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.min.js")}"></script>"""
  }

  def roundTag(implicit ctx: Context) =
    jsAt(s"compiled/lichess.round${isProd ?? (".min")}.js", async = true)

  val highchartsLatestTag = Html {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts.js")}"></script>"""
  }

  val highchartsMoreTag = Html {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts-more.js")}"></script>"""
  }

  val tagmanagerTag = Html {
    s"""<script src="${staticUrl("vendor/tagmanager/tagmanager.js")}"></script>"""
  }

  val typeaheadTag = Html {
    s"""<script src="${staticUrl("javascripts/vendor/typeahead.bundle.min.js")}"></script>"""
  }

  val fingerprintTag = Html {
    s"""<script async defer src="${staticUrl("javascripts/vendor/fp2.min.js")}"></script>"""
  }

  val flatpickrTag = Html {
    s"""<script async defer src="${staticUrl("javascripts/vendor/flatpickr.min.js")}"></script>"""
  }

  def basicCsp(implicit req: RequestHeader): ContentSecurityPolicy = {
    val assets = if (req.secure) "https://" + assetDomain else assetDomain
    val socket = (if (req.secure) "wss://" else "ws://") + socketDomain
    ContentSecurityPolicy(
      defaultSrc = List("'self'", assets),
      connectSrc = List("'self'", assets, socket + ":*", lila.api.Env.current.ExplorerEndpoint, lila.api.Env.current.TablebaseEndpoint),
      styleSrc = List("'self'", "'unsafe-inline'", assets, "https://fonts.googleapis.com"),
      fontSrc = List("'self'", assetDomain, "https://fonts.gstatic.com"),
      frameSrc = List("'self'", assets, "https://www.youtube.com"),
      workerSrc = List("'self'", assets),
      imgSrc = List("data:", "*"),
      scriptSrc = List("'self'", assets),
      baseUri = List("'none'")
    )
  }

  def defaultCsp(implicit ctx: Context): ContentSecurityPolicy = {
    val csp = basicCsp(ctx.req)
    ctx.nonce.fold(csp)(csp.withNonce(_))
  }

  def embedJsUnsafe(js: String)(implicit ctx: Context): Html = Html {
    val nonce = ctx.nonce ?? { nonce => s""" nonce="$nonce"""" }
    s"""<script$nonce>$js</script>"""
  }

  def embedJs(js: Html)(implicit ctx: Context): Html = embedJsUnsafe(js.body)
}
