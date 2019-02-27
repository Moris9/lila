package views.html.game

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.{ Game, Pov, Player }
import lila.user.Title

import controllers.routes

object bits {

  def featuredJs(pov: Pov) = Html {
    s"""${gameFenNoCtx(pov, tv = true)}${vstext(pov)(none)}"""
  }

  def mini(pov: Pov)(implicit ctx: Context) = Html {
    s"""${gameFen(pov)}${vstext(pov)(ctx.some)}"""
  }

  def miniBoard(fen: chess.format.FEN, color: chess.Color = chess.White) = Html {
    s"""<div class="mini-board parse-fen cg-board-wrap is2d" data-color="${color.name}" data-fen="$fen"><div class="cg-board"></div></div>"""
  }

  def watchers(implicit ctx: Context): Frag =
    div(
      cls := "chat__members none",
      aria.live := "off",
      aria.relevant := "additions removals text"
    )(
        span(cls := "number")(nbsp),
        " ",
        trans.spectators.txt().replace(":", ""),
        " ",
        span(cls := "list inline_userlist")
      )

  def gameIcon(game: Game): Char = game.perfType match {
    case _ if game.fromPosition => '*'
    case _ if game.imported => '/'
    case Some(p) if game.variant.exotic => p.iconChar
    case _ if game.hasAi => 'n'
    case Some(p) => p.iconChar
    case _ => '8'
  }

  def sides(
    pov: Pov,
    initialFen: Option[chess.format.FEN],
    tour: Option[lila.tournament.Tournament],
    cross: Option[lila.game.Crosstable.WithMatchup],
    simul: Option[lila.simul.Simul],
    userTv: Option[lila.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context) = div(cls := "sides")(
    side(pov, initialFen, tour, simul, userTv, bookmarked = bookmarked),
    cross.map { c =>
      div(cls := "crosstable")(crosstable(ctx.userId.fold(c)(c.fromPov), pov.gameId.some))
    }
  )

  def variantLink(
    variant: chess.variant.Variant,
    name: String,
    initialFen: Option[chess.format.FEN] = None
  ) = a(
    cls := "variant-link",
    href := (variant match {
      case chess.variant.Standard => "https://en.wikipedia.org/wiki/Chess"
      case chess.variant.FromPosition => s"""${routes.Editor.index}?fen=${initialFen.??(_.value.replace(' ', '_'))}"""
      case v => routes.Page.variant(v.key).url
    }),
    rel := "nofollow",
    target := "_blank",
    title := variant.title
  )(name)

  private def playerTitle(player: Player) =
    lightUser(player.userId).flatMap(_.title) map Title.apply map { t =>
      span(cls := "title", dataBot(t), title := Title titleName t)(t.value)
    }

  def vstext(pov: Pov)(ctxOption: Option[Context]) =
    div(cls := "vstext")(
      div(cls := "vstext__pl user_link")(
        playerUsername(pov.player, withRating = false, withTitle = false),
        br,
        playerTitle(pov.player) map { t => frag(t, " ") },
        pov.player.rating,
        pov.player.provisional option "?"
      ),
      pov.game.clock map { c =>
        div(cls := "vstext__clock")(shortClockName(c.config))
      } orElse {
        ctxOption flatMap { implicit ctx =>
          pov.game.daysPerTurn map { days =>
            div(cls := "vstext__clock")(
              if (days == 1) trans.oneDay.frag() else trans.nbDays.pluralSame(days)
            )
          }
        }
      },
      div(cls := "vstext__op user_link")(
        playerUsername(pov.opponent, withRating = false, withTitle = false),
        br,
        pov.opponent.rating,
        pov.opponent.provisional option "?",
        playerTitle(pov.opponent) map { t => frag(" ", t) }
      )
    )
}
