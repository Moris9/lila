package lila.relay

import org.joda.time.DateTime

import lila.user.User

case class RelayTour(
    _id: RelayTour.Id,
    name: String,
    description: String,
    markup: Option[String] = None,
    owner: User.ID,
    official: Boolean,
    createdAt: DateTime
) {
  def id = _id
}

object RelayTour {

  case class Id(value: String) extends AnyVal with StringValue

  def makeId = Id(lila.common.ThreadLocalRandom nextString 8)
}
