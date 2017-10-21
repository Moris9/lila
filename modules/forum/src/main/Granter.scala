package lila.forum

import lila.security.{ Permission, Granter => Master }
import lila.user.{ User, UserContext }

trait Granter {

  private val TeamSlugPattern = """^team-([\w-]+)$""".r

  protected def userBelongsToTeam(teamId: String, userId: String): Fu[Boolean]
  protected def userOwnsTeam(teamId: String, userId: String): Fu[Boolean]

  def isGrantedRead(categSlug: String)(implicit ctx: UserContext): Boolean =
    (categSlug == Categ.staffId).fold(
      ctx.me exists Master(Permission.StaffForum),
      true
    )

  def isGrantedWrite(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    ctx.me.filter(isOldEnoughToForum) ?? { me =>
      if (Master(Permission.StaffForum)(me)) fuccess(true)
      else categSlug match {
        case Categ.staffId => fuccess(false)
        case TeamSlugPattern(teamId) => userBelongsToTeam(teamId, me.id)
        case _ => fuccess(true)
      }
    }

  private def isOldEnoughToForum(u: User) = {
    u.count.game > 0 && u.createdSinceDays(2)
  } || u.hasTitle

  def isGrantedMod(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    categSlug match {
      case _ if (ctx.me ?? Master(Permission.ModerateForum)) => fuccess(true)
      case TeamSlugPattern(teamId) =>
        ctx.me ?? { me => userOwnsTeam(teamId, me.id) }
      case _ => fuccess(false)
    }
}
