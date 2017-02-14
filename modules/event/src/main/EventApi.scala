package lila.event

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo._

final class EventApi(
    coll: Coll,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import BsonHandlers._

  val promotable = asyncCache.single(
    name = "event.promotable",
    fetchPromotable,
    expireAfter = _.ExpireAfterWrite(5 minutes)
  )

  def fetchPromotable: Fu[List[Event]] = coll.find($doc(
    "enabled" -> true,
    "startsAt" $gt DateTime.now.minusDays(1) $lt DateTime.now.plusDays(1)
  )).sort($doc("startsAt" -> 1)).list[Event](10).map {
    _.filter(_.featureNow) take 3
  }

  def list = coll.find($empty).sort($doc("startsAt" -> -1)).list[Event](50)

  def recentEnabled = coll
    .find($doc("enabled" -> true))
    .sort($doc("startsAt" -> -1))
    .list[Event](50)

  def oneEnabled(id: String) = coll.byId[Event](id).map(_.filter(_.enabled))

  def one(id: String) = coll.byId[Event](id)

  def editForm(event: Event) = EventForm.form fill {
    EventForm.Data make event
  }

  def update(old: Event, data: EventForm.Data) =
    coll.update($id(old.id), data update old) >>- promotable.refresh

  def createForm = EventForm.form

  def create(data: EventForm.Data, userId: String): Fu[Event] = {
    val event = data make userId
    coll.insert(event) >>- promotable.refresh inject event
  }
}
