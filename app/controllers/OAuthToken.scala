package controllers

import play.api.libs.json.JsValue
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.oauth.AccessToken
import views._

object OAuthToken extends LilaController {

  private val env = Env.oAuth

  def index = Auth { implicit ctx => me =>
    env.tokenApi.list(me) map { tokens =>
      Ok(html.oAuthToken.index(tokens))
    }
  }

  def create = Auth { implicit ctx => me =>
    Ok(html.oAuthToken.create(env.forms.create)).fuccess
  }

  def createApply = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    env.forms.create.bindFromRequest.fold(
      err => BadRequest(html.oAuthToken.create(err)).fuccess,
      setup => env.tokenApi.create(setup make me) inject
        Redirect(routes.OAuthToken.index)
    )
  }

  def delete(id: String) = Auth { implicit ctx => me =>
    env.tokenApi.deleteBy(AccessToken.Id(id), me) inject
      Redirect(routes.OAuthToken.index)
  }
}
