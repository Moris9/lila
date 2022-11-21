package lila.common

import play.api.libs.json.*

case class LightUser(
    id: String,
    name: String,
    title: Option[String],
    isPatron: Boolean
):

  def titleName = title.fold(name)(_ + " " + name)

  def isBot = title has "BOT"

  def is(name: String) = id == LightUser.normalize(name)

object LightUser:

  type Ghost          = LightUser
  private type UserID = String

  val ghost: Ghost = LightUser("ghost", "ghost", none, false)

  given lightUserWrites: OWrites[LightUser] = OWrites { u =>
    writeNoId(u) + ("id" -> JsString(u.id))
  }

  def writeNoId(u: LightUser): JsObject =
    Json
      .obj("name" -> u.name)
      .add("title" -> u.title)
      .add("patron" -> u.isPatron)

  def fallback(name: String) =
    LightUser(
      id = normalize(name),
      name = name,
      title = None,
      isPatron = false
    )

  def normalize(name: String) = name.toLowerCase

  private type GetterType          = UserID => Fu[Option[LightUser]]
  opaque type Getter <: GetterType = GetterType
  object Getter:
    def apply(f: GetterType): Getter = f

  final class GetterSync(f: UserID => Option[LightUser]) extends (UserID => Option[LightUser]):
    def apply(u: UserID) = f(u)

  final class IsBotSync(f: UserID => Boolean) extends (UserID => Boolean):
    def apply(userId: UserID) = f(userId)
