package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object calendar {

  def apply(json: play.api.libs.json.JsObject)(implicit ctx: Context) = views.html.base.layout(
    title = "Tournament calendar",
    moreJs = frag(
      jsAt(s"compiled/lichess.tournamentCalendar${isProd ?? (".min")}.js"),
      embedJs(s"""LichessTournamentCalendar.app(document.getElementById('tournament-calendar'), {
data: ${safeJsonValue(json)},
i18n: ${bits.jsI18n()}
});""")
    ),
    moreCss = responsiveCssTag("tournament.calendar")
  ) {
      main(cls := "box")(
        h1("Tournament calendar"),
        div(id := "tournament-calendar")
      )
    }
}
