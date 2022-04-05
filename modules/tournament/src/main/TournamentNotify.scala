package lila.tournament

import akka.actor.ActorSystem
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Bus
import lila.common.{ AtMost, Every, ResilientScheduler }
import lila.hub.actorApi.push.TourSoon

final private class TournamentNotify(repo: TournamentRepo, cached: Cached, socket: TournamentSocket)(implicit
    ec: ExecutionContext,
    system: ActorSystem
) {

  private val doneMemo = new lila.memo.ExpireSetMemo(5 minutes)

  ResilientScheduler(every = Every(10 seconds), timeout = AtMost(10 seconds), initialDelay = 1 minute) {
    repo
      .soonStarting(DateTime.now.plusMinutes(0), DateTime.now.plusMinutes(60), doneMemo.keys)
      .flatMap {
        _.map { tour =>
          doneMemo put tour.id
          cached ranking tour map { ranking =>
            Bus.publish(
              TourSoon(tourId = tour.id, tourName = tour.name, ranking.playerIndex.toList),
              "tourSoon"
            )
          }
        }.sequenceFu.void
      }
  }
}
