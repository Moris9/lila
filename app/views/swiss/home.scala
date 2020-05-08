package views.html.swiss

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.swiss.Swiss

import controllers.routes

object home {

  def apply(
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "Swiss tournaments",
      moreCss = cssTag("swiss.home")
    ) {
      main(cls := "page-small box box-pad page swiss-home")(
        h1("Swiss tournaments [BETA]"),
        div(cls := "swiss-home__infos")(
          div(cls := "wiki")(
            iconTag(""),
            p(
              "In a Swiss tournament, each competitor does not necessarily play all other entrants. Competitors meet one-on-one in each round and are paired using a set of rules designed to ensure that each competitor plays opponents with a similar running score, but not the same opponent more than once. The winner is the competitor with the highest aggregate points earned in all rounds. All competitors play in each round unless there is an odd number of players."
            )
          ),
          div(cls := "team")(
            iconTag("f"),
            p(
              "Swiss tournaments can only be created by team leaders, and can only be played by team members.",
              br,
              a(href := routes.Team.home())("Join or create a team"),
              " to start playing in swiss tournaments."
            )
          ),
          div(cls := "faq")(
            i("?"),
            p(
              strong("How are points calculated?"),
              "A win is worth one point, a draw is a half point, and a loss is zero points.",
              br,
              "When a player can't be paired during a round, they receive a bye worth a half point"
            )
          ),
          div(cls := "faq")(
            i("?"),
            p(
              strong("How are tie breaks calculated?"),
              "With the ",
              a(
                href := "https://en.wikipedia.org/wiki/Tie-breaking_in_Swiss-system_tournaments#Sonneborn%E2%80%93Berger_score"
              )("Sonneborn–Berger score"),
              ".",
              br,
              "Add the scores of every opponent the player beats and half of the score of every opponent the player draws."
            )
          ),
          div(cls := "faq")(
            i("?"),
            p(
              strong("How are pairings decided?"),
              "With the ",
              a(
                href := "https://en.wikipedia.org/wiki/Swiss-system_tournament#Dutch_system"
              )("Dutch system"),
              ", implemented by ",
              a(href := "https://github.com/BieremaBoyzProgramming/bbpPairings")("bbPairings"),
              " in accordance with the ",
              a(href := "https://www.fide.com/fide/handbook.html?id=170&view=article")("FIDE handbook"),
              "."
            )
          ),
          div(cls := "faq")(
            i("?"),
            p(
              strong("What happens if the tournament has more rounds than players?"),
              "The tournament will just successfully end when all possible pairings have been played."
            )
          ),
          div(cls := "faq")(
            i("?"),
            p(
              strong("Why is it restricted to teams?"),
              "Swiss tournaments were not designed for online chess. They demand punctuality and consistency from players.",
              "We think these conditions are more likely to be met within a team than in global tournaments."
            )
          ),
          div(cls := "faq")(
            i("?"),
            p(
              strong("How many byes can a player get?"),
              "A player gets a bye every time the pairing system can't find a pairing for them.",
              br,
              "Additionally, a single bye is attributed when a player late-joins a tournament."
            )
          ),
          div(cls := "faq")(
            i("?"),
            p(
              strong("What happens if a player doesn't play a game?"),
              "Their clock will tick, they will flag, and lose the game.",
              br,
              "Then the system will withdraw the player from the tournament, so they don't lose more games.",
              br,
              "They can re-join the tournament at any time."
            )
          ),
          div(cls := "faq")(
            i("?"),
            p(
              strong("Can players late-join?"),
              "Yes, until half the rounds have been played.",
              br,
              "Late joiners get a single bye, even if they missed several rounds."
            )
          )
        )
      )
    }
}
