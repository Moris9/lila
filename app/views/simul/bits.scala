package views.html.simul

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object bits {

  def link(simulId: lila.simul.Simul.ID): Frag =
    a(href := routes.Simul.show(simulId))("Simultaneous exhibition")

  def jsI18n()(implicit ctx: Context) = safeJsonValue(i18nJsObject(baseTranslations))

  def notFound()(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.noSimulFound.txt()
    ) {
        main(cls := "page-small box box-pad")(
          h1(trans.noSimulFound()),
          p(trans.noSimulExplanation()),
          p(a(href := routes.Simul.home())(trans.returnToSimulHomepage()))
        )
      }

  def homepageSpotlight(s: lila.simul.Simul)(implicit ctx: Context) =
    a(href := routes.Simul.show(s.id), cls := "tour-spotlight little id_@s.id")(
      img(cls := "img icon", src := staticUrl("images/fire-silhouette.svg")),
      span(cls := "content")(
        span(cls := "name")(s.name, " simul"),
        span(cls := "more")(
          trans.nbPlayers.plural(s.applicants.size, s.applicants.size.localize),
          " • ",
          trans.join()
        )
      )
    )

  def allCreated(simuls: List[lila.simul.Simul]) =
    table(
      simuls map { simul =>
        tr(
          td(cls := "name")(
            a(cls := "text", href := routes.Simul.show(simul.id))(
              simul.perfTypes map { pt =>
                span(dataIcon := pt.iconChar)
              },
              simul.fullName
            )
          ),
          td(userIdLink(simul.hostId.some)),
          td(cls := "text", dataIcon := "p")(simul.clock.config.show),
          td(cls := "text", dataIcon := "r")(simul.applicants.size),
          td(a(href := routes.Simul.show(simul.id), cls := "button", dataIcon := "G"))
        )
      }
    )

  private[simul] def setup(sim: lila.simul.Simul)(implicit ctx: Context) =
    span(cls := List("setup" -> true, "rich" -> sim.variantRich))(
      sim.clock.config.show,
      " • ",
      sim.variants.map(_.name).mkString(", ")
    )

  private val baseTranslations = Vector(
    trans.finished,
    trans.withdraw,
    trans.join,
    trans.cancel,
    trans.joinTheGame,
    trans.nbPlaying,
    trans.nbWins,
    trans.nbDraws,
    trans.nbLosses,
    trans.by,
    trans.signIn
  )
}
