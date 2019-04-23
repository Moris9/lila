package views.html.user.show

import play.api.data.Form

import lila.api.Context
import lila.app.mashup.UserInfo.Angle
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.game.Game
import lila.user.User

import controllers.routes

object page {

  def activity(
    u: User,
    activities: Vector[lila.activity.ActivityView],
    info: lila.app.mashup.UserInfo,
    social: lila.app.mashup.UserInfo.Social
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${u.username} : ${trans.activity.activity.txt()}",
    openGraph = lila.app.ui.OpenGraph(
      image = staticUrl("images/large_tile.png").some,
      title = u.titleUsernameWithBestRating,
      url = s"$netBaseUrl${routes.User.show(u.username).url}",
      description = describeUser(u)
    ).some,
    moreJs = frag(
      jsAt("compiled/user.js"),
      info.ratingChart.map { ratingChart =>
        frag(
          jsTag("chart/ratingHistory.js"),
          embedJsUnsafe(s"lichess.ratingHistoryChart($ratingChart);")
        )
      },
      isGranted(_.UserSpy) option jsAt("compiled/user-mod.js")
    ),
    moreCss = frag(
      cssTag("user.show"),
      isGranted(_.UserSpy) option cssTag("mod.user")
    )
  ) {
      main(cls := "page-menu", dataUsername := u.username)(
        st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
        div(cls := "page-menu__content box user-show")(
          header(u, info, Angle.Activity, social),
          div(cls := "angle-content")(views.html.activity(u, activities))
        )
      )
    }

  def games(
    u: User,
    info: lila.app.mashup.UserInfo,
    games: Paginator[Game],
    filters: lila.app.mashup.GameFilterMenu,
    searchForm: Option[Form[_]],
    social: lila.app.mashup.UserInfo.Social
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${u.username} : ${userGameFilterTitleNoTag(u, info.nbs, filters.current)}${if (games.currentPage == 1) "" else " - page " + games.currentPage}",
    moreJs = frag(
      infiniteScrollTag,
      jsAt("compiled/user.js"),
      info.ratingChart.map { ratingChart =>
        frag(
          jsTag("chart/ratingHistory.js"),
          embedJsUnsafe(s"lichess.ratingHistoryChart($ratingChart);")
        )
      },
      filters.current.name == "search" option frag(jsTag("search.js")),
      isGranted(_.UserSpy) option jsAt("compiled/user-mod.js")
    ),
    moreCss = frag(
      cssTag("user.show"),
      if (filters.current.name == "search") cssTag("user.show.search")
      else info.nbs.crosstable.isDefined option cssTag("crosstable"),
      isGranted(_.UserSpy) option cssTag("mod.user")
    )
  ) {
      main(cls := "page-menu", dataUsername := u.username)(
        st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
        div(cls := "page-menu__content box user-show")(
          header(u, info, Angle.Games(searchForm), social),
          div(cls := "angle-content")(gamesContent(u, info.nbs, games, filters, filters.current.name))
        )
      )
    }

  def disabled(u: User)(implicit ctx: Context) =
    views.html.base.layout(title = u.username, robots = false) {
      main(cls := "box box-pad")(
        h1(u.username),
        p(trans.thisAccountIsClosed())
      )
    }

  private val dataUsername = attr("data-username")
}
