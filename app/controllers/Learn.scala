package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import views.html

import lila.api.Context
import lila.app.{ given, * }

final class Learn(env: Env) extends LilaController(env) {

  import lila.learn.JSONHandlers.given

  def index     = Open(serveIndex(_))
  def indexLang = LangPage(routes.Learn.index)(serveIndex(_)) _
  private def serveIndex(implicit ctx: Context) = NoBot {
    pageHit
    ctx.me
      .?? { me =>
        env.learn.api.get(me) map { Json.toJson(_) } map some
      }
      .map { progress =>
        Ok(html.learn.index(progress))
      }
  }

  private val scoreForm = Form(
    mapping(
      "stage" -> nonEmptyText,
      "level" -> number,
      "score" -> number
    )(Tuple3.apply)(unapply)
  )

  def score =
    AuthBody { implicit ctx => me =>
      implicit val body = ctx.body
      scoreForm
        .bindFromRequest()
        .fold(
          _ => BadRequest.toFuccess,
          { case (stage, level, s) =>
            val score = lila.learn.StageProgress.Score(s)
            env.learn.api.setScore(me, stage, level, score) >>
              env.activity.write.learn(me.id, stage) inject Ok(Json.obj("ok" -> true))
          }
        )
    }

  def reset =
    AuthBody { _ => me =>
      env.learn.api.reset(me) inject Ok(Json.obj("ok" -> true))
    }
}
