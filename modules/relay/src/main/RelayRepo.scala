package lila.relay

import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.db.dsl._

final private class RelayRepo(val coll: Coll) {

  import BSONHandlers._

  def scheduled =
    coll.ext
      .find(
        $doc(
          selectors scheduled true
        )
      )
      .sort($sort asc "startsAt")
      .list[Relay]()

  def ongoing =
    coll.ext
      .find(
        $doc(
          selectors ongoing true
        )
      )
      .sort($sort asc "startedAt")
      .list[Relay]()

  def finished =
    coll.ext
      .find(
        $doc(
          selectors finished true
        )
      )
      .sort($sort desc "startedAt")
      .list[Relay]()

  private[relay] object selectors {
    def scheduled(official: Boolean) = $doc(
      "startsAt" $gt DateTime.now.minusHours(1),
      "startedAt" $exists false,
      "official" -> official.option(true)
    )
    def ongoing(official: Boolean) = $doc(
      "startedAt" $exists true,
      "finished" -> false,
      "official" -> official.option(true)
    )
    def finished(official: Boolean) = $doc(
      "startedAt" $exists true,
      "finished" -> true,
      "official" -> official.option(true)
    )
  }
}
