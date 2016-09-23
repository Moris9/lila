package lila.coach

import org.joda.time.DateTime

import lila.user.User

case class Coach(
    _id: Coach.Id, // user ID
    listed: Coach.Listed,
    available: Coach.Available,
    approved: Coach.Approved,
    profile: CoachProfile,
    picturePath: Option[Coach.PicturePath],
    nbReviews: Int,
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id

  def is(user: User) = id.value == user.id

  def hasPicture = picturePath.isDefined

  def isListed = listed.value && approved.value
}

object Coach {

  def make(user: User) = Coach(
    _id = Id(user.id),
    listed = Listed(false),
    available = Available(true),
    approved = Approved(true),
    profile = CoachProfile(),
    picturePath = None,
    nbReviews = 0,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)

  case class WithUser(coach: Coach, user: User)

  case class Id(value: String) extends AnyVal with StringValue
  case class Listed(value: Boolean) extends AnyVal
  case class Available(value: Boolean) extends AnyVal
  case class Approved(value: Boolean) extends AnyVal
  case class PicturePath(value: String) extends AnyVal with StringValue
}
