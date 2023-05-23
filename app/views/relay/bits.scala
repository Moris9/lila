package views.html.relay

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.relay.RelayTour

object bits:

  def spotlight(tr: RelayTour.ActiveWithSomeRounds)(using Context) =
    a(
      href := tr.path,
      cls  := s"tour-spotlight event-spotlight relay-spotlight id_${tr.tour.id}"
    )(
      i(cls := "img", dataIcon := ""),
      span(cls := "content")(
        span(cls := "name")(tr.tour.name),
        span(cls := "more")(
          tr.display.caption.fold(tr.display.name.value)(_.value),
          " • ",
          if tr.display.hasStarted
          then trans.eventInProgress()
          else tr.display.startsAt.map(momentFromNow(_)) | "Soon"
        )
      )
    )

  def howToUse =
    a(dataIcon := "", cls := "text", href := routes.RelayTour.help)(
      "How to use Lichess Broadcasts"
    )

  def jsI18n(using Context) =
    views.html.study.jsI18n() ++
      i18nJsObject(i18nKeys)

  val i18nKeys =
    import trans.broadcast.*
    List(addRound, broadcastUrl, currentRoundUrl, currentGameUrl, downloadAllRounds)
