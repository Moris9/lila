package views.html.analyse

import controllers.routes
import play.api.libs.json.{ JsObject, Json }
import views.html.base.layout.{ bits => layout }

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object embed {

  import EmbedConfig.implicits._

  def apply(pov: lila.game.Pov, data: JsObject)(implicit config: EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.viewport,
          layout.metaCsp(basicCsp withNonce config.nonce),
          st.headTitle(replay titleOf pov),
          layout.pieceSprite(lila.pref.PieceSet.default),
          cssTagWithTheme("analyse.embed", config.bg)
        ),
        body(
          cls := s"highlight ${config.bg} ${config.board}",
          dataDev := netConfig.minifiedAssets.option("true"),
          dataAssetUrl := netConfig.assetBaseUrl,
          dataAssetVersion := assetVersion.value,
          dataTheme := config.bg
        )(
          div(cls := "is2d")(
            main(cls := "analyse")
          ),
          footer {
            val url = routes.Round.watcher(pov.gameId, pov.color.name)
            frag(
              div(cls := "left")(
                a(target := "_blank", href := url)(h1(titleGame(pov.game))),
                " ",
                em("brought to you by ", a(target := "_blank", href := netBaseUrl)(netConfig.domain))
              ),
              a(target := "_blank", cls := "open", href := url)("Open")
            )
          },
          jQueryTag,
          jsTag("vendor/mousetrap.js"),
          jsModule("analyse.embed"),
          analyseTag,
          embedJsUnsafe(
            s"""analyseEmbedOpts=${safeJsonValue(
              Json.obj(
                "data"  -> data,
                "embed" -> true,
                "i18n"  -> views.html.board.userAnalysisI18n(withCeval = false, withExplorer = false)
              )
            )}""",
            config.nonce
          )
        )
      )
    )

  def notFound(implicit config: EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.viewport,
          layout.metaCsp(basicCsp),
          st.headTitle("404 - Game not found"),
          cssTagWithTheme("analyse.embed", "dark")
        ),
        body(cls := "dark")(
          div(cls := "not-found")(
            h1("Game not found")
          )
        )
      )
    )
}
