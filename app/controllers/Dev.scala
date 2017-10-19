package controllers

import akka.pattern.ask
import play.api.data._, Forms._
import play.api.libs.json._
import play.api.mvc._

import lila.app._
import lila.common.HTTPRequest
import views._

object Dev extends LilaController {

  def assetVersion = SecureBody(_.AssetVersion) { implicit ctx => me =>
    getInt("version") match {
      case None => Ok(html.dev.assetVersion(
        Env.api.assetVersion.fromConfig,
        Env.api.assetVersion.get
      )).fuccess
      case Some(v) => Env.api.assetVersion.set(
        lila.common.AssetVersion(v)
      ) inject Redirect(routes.Dev.assetVersion)
    }
  }
}
