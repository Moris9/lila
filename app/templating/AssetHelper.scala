package lila.app
package templating

import controllers.routes
import play.api.templates.Html

trait AssetHelper {

  val assetVersion = lila.api.Env.current.Net.AssetVersion

  def isProd: Boolean

  private val domain = lila.api.Env.current.Net.AssetDomain

  def staticUrl(path: String) = s"http://$domain${routes.Assets.at(path)}"

  def cssTag(name: String) = css("stylesheets/" + name)

  def cssVendorTag(name: String) = css("vendor/" + name)

  private def css(path: String) = Html {
    s"""<link href="${staticUrl(path)}?v=$assetVersion" type="text/css" rel="stylesheet"/>"""
  }

  def jsTag(name: String) = jsAt("javascripts/" + name)

  def jsTagCompiled(name: String) = if (isProd) jsAt("compiled/" + name) else jsTag(name)

  lazy val highchartsTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/3.0/highcharts.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts/highcharts.js"))

  lazy val highchartsMoreTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/3.0/highcharts-more.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts/highcharts-more.js"))

  lazy val highstockTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/stock/3.0/highstock.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highstock/highstock.js"))

  private def cdnOrLocal(cdn: String, test: String, local: String) = Html {
    s"""<script src="$cdn"></script><script>$test || document.write('<script src="$local?v=$assetVersion">\\x3C/script>')</script>"""
  }

  def jsAt(path: String, static: Boolean = true) = Html {
    s"""<script src="${static.fold(staticUrl(path), path)}?v=$assetVersion"></script>"""
  }

  def embedJs(js: String): Html = Html(s"""<script type="text/javascript">/* <![CDATA[ */ $js /* ]]> */</script>""")
}
