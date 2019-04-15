package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.user.User

import controllers.routes

object insight {

  def index(
    u: User,
    cache: lila.insight.UserCache,
    prefId: Int,
    ui: play.api.libs.json.JsObject,
    question: play.api.libs.json.JsObject,
    stale: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username}'s chess insights",
      moreJs = frag(
        highchartsLatestTag,
        jsAt("vendor/multiple-select/multiple-select.js"),
        jsAt(s"compiled/lichess.insight${isProd ?? (".min")}.js"),
        jsTag("insight-refresh.js"),
        jsTag("insight-tour.js"),
        embedJs(s"""
$$(function() {
lichess = lichess || {};
lichess.insight = LichessInsight(document.getElementById('insight'), {
ui: ${safeJsonValue(ui)},
initialQuestion: ${safeJsonValue(question)},
i18n: {},
myUserId: $jsUserId,
user: { id: "${u.id}", name: "${u.username}", nbGames: ${cache.count}, stale: ${stale}, shareId: ${prefId} },
pageUrl: "${routes.Insight.index(u.username)}",
postUrl: "${routes.Insight.json(u.username)}"
});
});""")
      ),
      moreCss = responsiveCssTag("insight")
    )(frag(
        main(id := "insight"),
        stale option div(cls := "insight-stale none")(
          p("There are new games to learn from!"),
          refreshForm(u, "Update insights")
        )
      ))

  def empty(u: User)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username}'s chess insights",
      moreJs = jsTag("insight-refresh.js")
    )(
        main(cls := "box box-pad page-small")(
          h1(cls := "text", dataIcon := "7")(u.username, " chess insights"),
          p(userLink(u), " has no chess insights yet!"),
          refreshForm(u, s"Generate ${u.username}'s chess insights")
        )
      )

  def forbidden(u: User)(implicit ctx: Context) =
    views.html.site.message(
      title = s"${u.username}'s chess insights are protected",
      back = true,
      icon = "7".some
    )(
      p("Sorry, you cannot see ", userLink(u), "'s chess insights."),
      br,
      p(
        "Maybe ask them to change their ",
        a(cls := "button", href := routes.Pref.form("privacy"))("privacy settings"),
        " ?"
      )
    )

  def refreshForm(u: User, action: String)(implicit ctx: Context) =
    form(cls := "insight-refresh", method := "post", st.action := routes.Insight.refresh(u.username))(
      button(dataIcon := "E", cls := "button text")(action),
      div(cls := "crunching none")(
        spinner,
        br,
        p(strong("Now crunching data just for you!"))
      )
    )
}
