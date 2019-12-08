package views
package html.site

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object freeJs {

  private lazy val agpl = a(href := "https://www.gnu.org/licenses/agpl-3.0.en.html")("AGPL-3.0+")

  private def github(path: String) = a(href := s"https://github.com/ornicar/lila/tree/master/$path")(path)

  private val uiModules = List("site", "chat", "cli", "challenge", "notify", "learn", "insight", "editor", "puzzle", "round", "analyse", "lobby", "tournament", "tournamentSchedule", "tournamentCalendar", "simul", "perfStat", "dasher")

  def apply(): Frag = frag(
    div(cls := "box__top")(
      h1("JavaScript modules")
    ),
    p(cls := "box__pad")(
      "Here are all frontend modules from ",
      a(href := "https://github.com/ornicar/lila/tree/master/ui")("ornicar/lila ui"),
      " in ",
      a(href := "https://www.gnu.org/licenses/javascript-labels.en.html")("Web Labels"),
      " compatible format:"
    ),
    table(id := "jslicense-labels1", cls := "slist slist-pad")(
      thead(
        tr(List("Script File", "License", "Source Code").map(th(_)))
      ),
      tbody(
        uiModules map { module =>
          val file = s"lichess.$module.min.js"
          tr(
            td(a(href := assetUrl(s"compiled/$file"))(file)),
            td(agpl),
            td(github(s"ui/$module/src"))
          )
        }
      )
    )
  )
}
