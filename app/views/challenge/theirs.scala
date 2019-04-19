package views.html.challenge

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge.Status

import controllers.routes

object theirs {

  def apply(
    c: lila.challenge.Challenge,
    json: play.api.libs.json.JsObject,
    user: Option[lila.user.User]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      moreJs = bits.js(c, json, false),
      moreCss = responsiveCssTag("challenge.page")
    ) {
        main(cls := "page-small challenge-page challenge-theirs box box-pad")(
          c.status match {
            case Status.Created | Status.Offline => frag(
              h1(user.fold[Frag]("Anonymous")(u =>
                frag(
                  userLink(u),
                  " (", u.perfs(c.perfType).glicko.display, ")"
                ))),
              bits.details(c),
              c.initialFen.map { fen =>
                div(cls := "board-preview", views.html.game.bits.miniBoard(fen, color = !c.finalColor))
              },
              if (!c.mode.rated || ctx.isAuth) frag(
                (c.mode.rated && c.unlimited) option
                  badTag(trans.bewareTheGameIsRatedButHasNoClock.frag()),
                form(cls := "accept", action := routes.Challenge.accept(c.id), method := "post")(
                  button(tpe := "submit", cls := "text button button-fat", dataIcon := "G")(trans.joinTheGame.frag())
                )
              )
              else frag(
                hr,
                badTag(
                  p("This game is rated"),
                  p(
                    "You must ",
                    a(cls := "button", href := s"${routes.Auth.login}?referrer=${routes.Round.watcher(c.id, "white")}")(trans.signIn.frag()),
                    " to join it."
                  )
                )
              )
            )
            case Status.Declined => div(cls := "follow-up")(
              h1("Challenge declined"),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent.frag())
            )
            case Status.Accepted => div(cls := "follow-up")(
              h1("Challenge accepted!"),
              bits.details(c),
              a(id := "challenge-redirect", href := routes.Round.watcher(c.id, "white"), cls := "button button-fat")(
                trans.joinTheGame.frag()
              )
            )
            case Status.Canceled => div(cls := "follow-up")(
              h1("Challenge canceled."),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent.frag())
            )
          }
        )
      }
}
