package lila.report

import lila.user.User

case class Mod(user: User) extends AnyVal

case class Suspect(user: User) extends AnyVal {

  def set(f: User => User) = copy(user = f(user))
}

case class Victim(user: User) extends AnyVal

case class Reporter(user: User) extends AnyVal {
  def id = ReporterId(user.id)
}
case class ReporterId(userId: User.ID) extends AnyVal

object ReporterId {
  implicit val reporterIdIso = lila.common.Iso.string[ReporterId](ReporterId.apply, _.userId)
}
