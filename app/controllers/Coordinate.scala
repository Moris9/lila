package controllers

import play.api.mvc.Result

import lila.api.Context
import lila.app._

final class Coordinate(env: Env) extends LilaController(env) {

  def home     = Open(serveHome(_))
  def homeLang = LangPage(routes.Coordinate.home)(serveHome(_)) _
  private def serveHome(implicit ctx: Context): Fu[Result] =
    ctx.userId ?? { userId =>
      env.coordinate.api getScore userId map (_.some)
    } map { score =>
      views.html.coordinate.show(score)
    }

  def score =
    AuthBody { implicit ctx => me =>
      implicit val body = ctx.body
      env.coordinate.forms.score
        .bindFromRequest()
        .fold(
          _ => fuccess(BadRequest),
          data => env.coordinate.api.addScore(me.id, data.mode, data.color, data.score) inject Ok(())
        )
    }
}
