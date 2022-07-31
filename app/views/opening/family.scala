package views.html.opening

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.LilaOpeningFamily
import lila.common.String.html.safeJsonValue
import lila.opening.OpeningFamilyData
import lila.puzzle.PuzzleOpening

object family {

  def apply(fam: LilaOpeningFamily, data: OpeningFamilyData, puzzle: Option[PuzzleOpening.FamilyWithCount])(
      implicit ctx: Context
  ) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = frag(
        jsModule("opening"),
        embedJsUnsafeLoadThen {
          import lila.opening.OpeningHistory.historyJsonWrite
          s"""LichessOpening.family(${safeJsonValue(Json.obj("history" -> data.history))})"""
        }
      ),
      title = s"${trans.opening.txt()} • ${fam.name}",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(fam.full.fen.replace(" ", "_"), chess.White.name, fam.full.uci.split(" ").lastOption, none).url}"
          ).some,
          title = fam.name.value,
          url = s"$netBaseUrl${routes.Opening.family(fam.key.value)}",
          description = fam.full.pgn
        )
        .some
    ) {
      main(cls := "page box box-pad opening")(
        h1(a(href := routes.Opening.index, dataIcon := "", cls := "text"), fam.name.value),
        h2(fam.full.pgn),
        div(
          cls              := "replay replay--autoload",
          st.data("pgn")   := fam.full.pgn,
          st.data("title") := fam.full.name
        ),
        canvas(cls := "opening__popularity"),
        puzzle.map { p =>
          a(cls := "button", href := routes.Puzzle.show(p.family.key.value))("Train with puzzles")
        },
        a(cls := "button", href := s"${routes.UserAnalysis.pgn(fam.full.pgn.replace(" ", "_"))}#explorer")(
          "View in opening explorer"
        )
      )
    }
}
