package views.html.board

import chess.format.{ Fen, Uci, BoardFen, Forsyth }
import controllers.routes
import play.api.libs.json.{ JsObject, JsString, Json }

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.game.Pov

object bits:

  private val dataState = attr("data-state")

  private def miniOrientation(pov: Pov): chess.Color =
    if (pov.game.variant == chess.variant.RacingKings) chess.White else pov.player.color

  def mini(pov: Pov): Tag => Tag =
    mini(
      Forsyth.exportBoard(pov.game.board),
      miniOrientation(pov),
      pov.game.history.lastMove
    )

  def mini(fen: BoardFen, color: chess.Color = chess.White, lastMove: Option[Uci] = None)(tag: Tag): Tag =
    tag(
      cls       := "mini-board mini-board--init cg-wrap is2d",
      dataState := s"${fen.value},${color.name},$lastMove"
    )(cgWrapContent)

  def miniSpan(fen: BoardFen, color: chess.Color = chess.White, lastMove: Option[Uci] = None) =
    mini(fen, color, lastMove)(span)

  private def explorerConfig(using ctx: Context) = Json.obj(
    "endpoint"          -> explorerEndpoint,
    "tablebaseEndpoint" -> tablebaseEndpoint,
    "showRatings"       -> ctx.pref.showRatings
  )
  def explorerAndCevalConfig(using ctx: Context) =
    Json.obj(
      "explorer"               -> explorerConfig,
      "externalEngineEndpoint" -> externalEngineEndpoint
    )
