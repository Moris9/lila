package views.html.challenge

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge.Status

import controllers.routes

object mine {

  def apply(c: lila.challenge.Challenge, json: play.api.libs.json.JsObject, error: Option[String])(implicit ctx: Context) = {

    val cancelForm =
      form(method := "post", action := routes.Challenge.cancel(c.id), cls := "cancel xhr")(
        button(tpe := "submit", cls := "button button-red text", dataIcon := "L")(trans.cancel.frag())
      )

    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      moreJs = bits.js(c, json, true),
      moreCss = responsiveCssTag("challenge.page")
    ) {
        main(cls := "page-small challenge-page box box-pad")(
          c.status match {
            case Status.Created | Status.Offline => div(id := "ping-challenge")(
              h1(trans.challengeToPlay.frag()),
              bits.details(c),
              c.destUserId.map { destId =>
                div(cls := "waiting")(
                  userIdLink(destId.some, cssClass = "target".some),
                  spinner,
                  p(trans.waitingForOpponent.frag())
                )
              } getOrElse div(cls := "invite")(
                div(
                  p(trans.toInviteSomeoneToPlayGiveThisUrl(), ": "),
                  p(cls := "challenge-id-form")(
                    input(
                      id := "challenge-id",
                      cls := "copyable autoselect",
                      spellcheck := "false",
                      readonly,
                      value := s"$netBaseUrl${routes.Round.watcher(c.id, "white")}"
                    ),
                    button(title := "Copy URL", cls := "copy button", dataRel := "challenge-id", dataIcon := "\"")
                  ),
                  p(trans.theFirstPersonToComeOnThisUrlWillPlayWithYou.frag())
                ),
                ctx.isAuth option div(
                  p("Or invite a lichess user:"),
                  form(cls := "user-invite", action := routes.Challenge.toFriend(c.id), method := "POST")(
                    input(name := "username", cls := "friend-autocomplete", placeholder := trans.search.txt()),
                    error.map { badTag(_) }
                  )
                )
              ),
              c.notableInitialFen.map { fen =>
                frag(
                  br,
                  div(cls := "board-preview", views.html.game.bits.miniBoard(fen, color = c.finalColor))
                )
              },
              cancelForm
            )
            case Status.Declined => div(cls := "follow-up")(
              h1("Challenge declined"),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent.frag())
            )
            case Status.Accepted => div(cls := "follow-up")(
              h1("Challenge accepted!"),
              bits.details(c),
              a(id := "challenge-redirect", href := routes.Round.watcher(c.id, "white"), cls := "button-fat")(
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
}
