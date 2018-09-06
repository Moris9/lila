package lila.relay

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import lila.user.User
import lila.security.Granter

object RelayForm {

  import lila.common.Form.UTCDate._

  val form = Form(mapping(
    "name" -> nonEmptyText(minLength = 3, maxLength = 80),
    "description" -> nonEmptyText(minLength = 3, maxLength = 4000),
    "official" -> boolean,
    "syncUrl" -> nonEmptyText,
    "startsAt" -> optional(utcDate),
    "throttle" -> optional(number(min = 0, max = 60))
  )(Data.apply)(Data.unapply))

  def create = form

  def edit(r: Relay) = form fill Data.make(r)

  case class Data(
      name: String,
      description: String,
      official: Boolean,
      syncUrl: String,
      startsAt: Option[DateTime],
      throttle: Option[Int]
  ) {

    def cleanUrl = {
      val trimmed = syncUrl.trim
      if (trimmed endsWith "/") trimmed.take(trimmed.size - 1)
      else trimmed
    }

    def update(relay: Relay, user: User) = relay.copy(
      name = name,
      description = description,
      official = official && Granter(_.Relay)(user),
      sync = makeSync,
      startsAt = startsAt
    )

    def makeSync = Relay.Sync(
      upstream = Relay.Sync.Upstream(cleanUrl),
      until = none,
      nextAt = none,
      delay = throttle,
      log = SyncLog.empty
    )

    def make(user: User) = Relay(
      _id = Relay.makeId,
      name = name,
      description = description,
      ownerId = user.id,
      sync = makeSync,
      likes = lila.study.Study.Likes(1),
      createdAt = DateTime.now,
      finished = false,
      official = official && Granter(_.Relay)(user),
      startsAt = startsAt,
      startedAt = none
    )
  }

  object Data {

    def make(relay: Relay) = Data(
      name = relay.name,
      description = relay.description,
      official = relay.official,
      syncUrl = relay.sync.upstream.url,
      startsAt = relay.startsAt,
      throttle = relay.sync.delay
    )
  }
}
