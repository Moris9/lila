package views.html.opening

import chess.opening.FullOpening
import controllers.routes
import play.api.libs.json.{ JsArray, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.opening.OpeningConfig
import lila.opening.{ OpeningPage, OpeningQuery }
import lila.puzzle.PuzzleOpening

object index {

  import bits._

  def apply(page: OpeningPage, wikiMissing: List[FullOpening])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = moreJs(page.some),
      title = trans.opening.txt(),
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(page.query.fen.value, chess.White.name, none, none, ctx.pref.theme.some, ctx.pref.pieceSet.some).url}"
          ).some,
          title = "Chess openings",
          url = s"$netBaseUrl${routes.Opening.index()}",
          description = "Explore the chess openings"
        )
        .some,
      csp = defaultCsp.withInlineIconFont.some
    ) {
      main(cls := "page box box-pad opening opening--index")(
        searchAndConfig(page.query.config, "", "index"),
        h1("Chess openings (beta)"),
        search.resultsList(Nil),
        page.explored.map(whatsNext) |
          p(cls := "opening__error")("Couldn't fetch the next moves, try again later."),
        isGranted(_.OpeningWiki) option wiki.showMissing(wikiMissing)
      )
    }

  def searchAndConfig(config: OpeningConfig, q: String, thenTo: String, searchFocus: Boolean = false)(implicit
      ctx: Context
  ) =
    div(cls := "opening__search-config")(
      search.form(q, searchFocus),
      configForm(config, thenTo)
    )
}
