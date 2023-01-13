package views.html.tutor

import controllers.routes
import play.api.libs.json.*

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.tutor.{ TutorFullReport, TutorPerfReport }
import lila.insight.Phase
import lila.insight.InsightPosition

object skills:

  def apply(full: TutorFullReport.Available, report: TutorPerfReport, user: lila.user.User)(using Context) =
    bits.layout(full, menu = perf.menu(full, user, report, "skills"))(
      cls := "tutor__skills box",
      boxTop(
        h1(
          a(href := routes.Tutor.perf(user.username, report.perf.key), dataIcon := "", cls := "text"),
          bits.otherUser(user),
          report.perf.trans,
          " skills"
        )
      ),
      bits.mascotSays(
        ul(report skillHighlights 3 map compare.show)
      ),
      div(cls := "tutor__pad")(
        grade.peerGradeWithDetail(concept.accuracy, report.accuracy, InsightPosition.Move),
        hr,
        grade.peerGradeWithDetail(concept.tacticalAwareness, report.awareness, InsightPosition.Move),
        hr,
        grade.peerGradeWithDetail(concept.resourcefulness, report.resourcefulness, InsightPosition.Game),
        hr,
        grade.peerGradeWithDetail(concept.conversion, report.conversion, InsightPosition.Game)
      )
    )
