package views.html.tutor

import controllers.routes
import play.api.libs.json.*

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.tutor.{ TutorFullReport, TutorQueue }
import lila.user.User
import lila.game.Pov
import play.api.i18n.Lang

object empty:

  def start(user: User)(using Context) =
    bits.layout(TutorFullReport.Empty(TutorQueue.NotInQueue), menu = emptyFrag, pageSmall = true)(
      cls := "tutor__empty box",
      boxTop(h1("Lichess Tutor")),
      bits.mascotSays("Explain what tutor is about here."),
      postForm(cls := "tutor__empty__cta", action := routes.Tutor.refresh(user.username))(
        submitButton(cls := "button button-fat button-no-upper")("Analyse my games and help me improve")
      )
    )

  def queued(in: TutorQueue.InQueue, user: User, waitGames: List[(Pov, PgnStr)])(using Context) =
    bits.layout(
      TutorFullReport.Empty(in),
      menu = emptyFrag,
      title = "Lichess Tutor - Examining games...",
      pageSmall = true
    )(
      data("eta") := (in.avgDuration.toMillis atMost 60_000 atLeast 10_000),
      cls         := "tutor__empty tutor__queued box",
      boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
      if (in.position == 1)
        bits.mascotSays(
          p(strong(cls := "tutor__intro")("I'm examining your games now!")),
          examinationMethod,
          nbGames(user),
          p("It should be done in a minute or two.")
        )
      else
        bits.mascotSays(
          p(strong(cls := "tutor__intro")("I will examine your games as soon as possible.")),
          examinationMethod,
          nbGames(user),
          p(
            "There are ",
            (in.position - 1),
            " players in the queue before you.",
            br,
            "You will get your results in about ",
            showMinutes(in.eta.toMinutes.toInt atLeast 1),
            "."
          )
        ),
      div(cls := "tutor__waiting-games")(
        div(cls := "tutor__waiting-games__carousel")(waitGames.map(waitGame))
      )
    )

  private def waitGame(game: (Pov, PgnStr))(using Context) =
    div(
      cls            := "tutor__waiting-game lpv lpv--todo lpv--moves-false lpv--controls-false",
      st.data("pgn") := game._2.value,
      st.data("pov") := game._1.color.name
    )

  private def nbGames(user: User)(using Lang) = {
    val nb = lila.rating.PerfType.standardWithUltra.foldLeft(0) { (nb, pt) =>
      nb + user.perfs(pt).nb
    }
    p(s"Looks like you have ", strong(nb.atMost(10_000).localize), " rated games to look at, excellent!")
  }

  private def examinationMethod = p(
    "Using the best chess engine: ",
    views.html.plan.features.engineFullName,
    ", ",
    "and comparing your playstyle to thousands of other players with similar rating."
  )

  def insufficientGames(using Context) =
    bits.layout(TutorFullReport.InsufficientGames, menu = emptyFrag, pageSmall = true)(
      cls := "tutor__insufficient box",
      boxTop(h1("Lichess Tutor")),
      mascotSaysInsufficient
    )

  def mascotSaysInsufficient =
    bits.mascotSays("Not enough rated games to examine! Go and play some more chess.")
