package views.html
package round

import scala.util.chaining._

import chess.variant.{ Crazyhouse, Variant }
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.{ Game, Pov }

import controllers.routes

object bits {

  def layout(
      variant: Variant,
      title: String,
      moreJs: Frag = emptyFrag,
      openGraph: Option[lila.app.ui.OpenGraph] = None,
      moreCss: Frag = emptyFrag,
      chessground: Boolean = true,
      playing: Boolean = false,
      robots: Boolean = false
  )(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      openGraph = openGraph,
      moreJs = moreJs,
      moreCss = frag(
        cssTag { if (variant == Crazyhouse) "round.zh" else "round" },
        ctx.blind option cssTag("round.nvui"),
        moreCss
      ),
      chessground = chessground,
      playing = playing,
      robots = robots,
      deferJs = true,
      zoomable = true,
      csp = defaultCsp.withPeer.some
    )(body)

  def crosstable(cross: Option[lila.game.Crosstable.WithMatchup], game: Game)(implicit ctx: Context) =
    cross map { c =>
      views.html.game.crosstable(ctx.userId.fold(c)(c.fromPov), game.id.some)
    }

  def underchat(game: Game)(implicit ctx: Context) =
    frag(
      div(
        cls := "chat__members none",
        aria.live := "off",
        aria.relevant := "additions removals text"
      )(
        span(cls := "number")(nbsp),
        " ",
        trans.spectators.txt().replace(":", ""),
        " ",
        span(cls := "list")
      ),
      isGranted(_.ViewBlurs) option div(cls := "round__mod")(
        game.players.filter(p => game.playerBlurPercent(p.color) > 30) map { p =>
          div(
            playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, withOnline = false, mod = true),
            s" ${p.blurs.nb}/${game.playerMoves(p.color)} blurs ",
            strong(game.playerBlurPercent(p.color), "%")
          )
        }
        // game.players flatMap { p => p.holdAlert.map(p ->) } map {
        //   case (p, h) => div(
        //     playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, mod = true, withOnline = false),
        //     "hold alert",
        //     br,
        //     s"(ply: ${h.ply}, mean: ${h.mean} ms, SD: ${h.sd})"
        //   )
        // }
      )
    )

  def others(playing: List[Pov], simul: Option[lila.simul.Simul])(implicit ctx: Context) =
    frag(
      h3(
        simul.map { s =>
          span(cls := "simul")(
            a(href := routes.Simul.show(s.id))("SIMUL"),
            span(cls := "win")(s.wins, " W"),
            " / ",
            span(cls := "draw")(s.draws, " D"),
            " / ",
            span(cls := "loss")(s.losses, " L"),
            " / ",
            s.ongoing,
            " ongoing"
          )
        } getOrElse trans.currentGames(),
        "round-toggle-autoswitch" pipe { id =>
          span(cls := "move-on switcher", st.title := trans.automaticallyProceedToNextGameAfterMoving.txt())(
            label(`for` := id)(trans.autoSwitch()),
            span(cls := "switch")(form3.cmnToggle(id, id, checked = false))
          )
        }
      ),
      div(cls := "now-playing")(
        playing.partition(_.isMyTurn) pipe {
          case (myTurn, otherTurn) =>
            (myTurn ++ otherTurn.take(6 - myTurn.size)) take 9 map {
              views.html.game.mini(_)
            }
        }
      )
    )

  private[round] def side(
      pov: Pov,
      data: play.api.libs.json.JsObject,
      tour: Option[lila.tournament.TourAndTeamVs],
      simul: Option[lila.simul.Simul],
      userTv: Option[lila.user.User] = None,
      bookmarked: Boolean
  )(implicit ctx: Context) =
    views.html.game.side(
      pov,
      (data \ "game" \ "initialFen").asOpt[String].map(chess.format.FEN),
      tour,
      simul = simul,
      userTv = userTv,
      bookmarked = bookmarked
    )

  def roundAppPreload(pov: Pov, controls: Boolean)(implicit ctx: Context) =
    div(cls := "round__app")(
      div(cls := "round__app__board main-board")(chessground(pov)),
      div(cls := "round__app__table"),
      div(cls := "ruser ruser-top user-link")(i(cls := "line"), a(cls := "text")(playerText(pov.opponent))),
      div(cls := "ruser ruser-bottom user-link")(i(cls := "line"), a(cls := "text")(playerText(pov.player))),
      div(cls := "rclock rclock-top preload")(div(cls := "time")(nbsp)),
      div(cls := "rclock rclock-bottom preload")(div(cls := "time")(nbsp)),
      div(cls := "rmoves")(div(cls := "moves")),
      controls option div(cls := "rcontrols rcontrols-preload")
    )
}
