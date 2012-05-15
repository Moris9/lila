package lila
package setup

import http.Context

import play.api.data._
import play.api.data.Forms._
import scalaz.effects._

final class FormFactory(
  configRepo: UserConfigRepo) {

  def aiFilled(implicit ctx: Context): IO[Form[AiConfig]] =
    aiConfig map ai.fill

  def ai = Form(
    mapping(
      "variant" -> number.verifying(Config.variants contains _),
      "level" -> number,
      "color" -> nonEmptyText.verifying(Color.names contains _)
    )(AiConfig.<<)(_.>>)
  )

  def aiConfig(implicit ctx: Context): IO[AiConfig] = ctx.me.fold(
    user ⇒ configRepo.config(user) map (_.ai),
    io(AiConfig.default)
  )
}
