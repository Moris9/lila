package views.html.tv

import play.api.mvc.RequestHeader

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import views.html.base.layout.bits

import controllers.routes

object embed {

  private val dataStreamUrl = attr("data-stream-url")

  def apply(pov: lila.game.Pov, bg: String, board: String)(implicit req: RequestHeader) = frag(
    bits.doctype,
    html(
      head(
        bits.charset,
        bits.viewport,
        bits.metaCsp(basicCsp),
        st.headTitle("lichess.org chess TV"),
        bits.pieceSprite(lila.pref.PieceSet.default),
        responsiveCssTagWithTheme("tv.embed", bg)
      ),
      body(
        cls := s"base $board merida",
        dataStreamUrl := routes.Tv.feed
      )(
          div(id := "featured-game", cls := "embedded", title := "lichess.org TV")(
            gameFenNoCtx(pov, tv = true, blank = true),
            views.html.game.bits.vstext(pov)(none)
          ),
          jQueryTag,
          jsAt("javascripts/vendor/chessground.min.js", false),
          jsAt("compiled/tv.js", false)
        )
    )
  )
}
