package lila.user

import play.api.mvc.Cookie

import lila.common.LilaCookie

final class Setting(ctx: Context) {

  def theme = Theme(get("theme"))
  def theme(value: String) = set("theme", Theme(value).toString)

  def sound = get("sound", "false").parseBoolean | false
  def sound(value: String) = set("sound", value)

  def bg = get("bg", "light")
  def bg(value: String) = set("bg", value)

  private def get(name: String, default: String = ""): String =
    ctx.req.session get name orElse {
      ctx.me flatMap (_ setting name) map (_.toString)
    } getOrElse default

  private def set(name: String, value: String): Fu[Cookie] =
    ctx.me ?? { m ⇒
      UserRepo.saveSetting(m.id, name, value.toString)
    } inject LilaCookie.session(name, value.toString)(ctx.req)
}

object Setting {

  def apply(ctx: Context) = new Setting(ctx)
}
