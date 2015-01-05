package lila.tournament

import scala.concurrent.duration.FiniteDuration

import lila.db.BSON._
import lila.user.{ User, UserRepo }

final class Winners(
    mongoCache: lila.memo.MongoCache.Builder,
    ttl: FiniteDuration) {

  private implicit val WinnerBSONHandler =
    reactivemongo.bson.Macros.handler[Winner]

  private val scheduledCache = mongoCache[Int, List[Winner]](
    prefix = "tournament:winner",
    f = fetchScheduled,
    timeToLive = ttl)

  import Schedule.Freq
  private def fetchScheduled(nb: Int): Fu[List[Winner]] =
    List(Freq.Monthly, Freq.Weekly, Freq.Daily).map { freq =>
      TournamentRepo.lastFinishedScheduledByFreq(freq, 3) flatMap toursToWinners
    }.sequenceFu map (_.flatten) flatMap { winners =>
      TournamentRepo.lastFinishedScheduledByFreq(
        Freq.Hourly, math.max(0, nb - winners.size)
      ) flatMap toursToWinners map (winners ::: _)
    }

  private def toursToWinners(tours: List[Finished]): Fu[List[Winner]] =
    tours.flatMap { tour =>
      tour.winner map { w =>
        Winner(tour.id, tour.name, w.id)
      }
    }.map { winner =>
      UserRepo isEngine winner.userId map (!_ option winner)
    }.sequenceFu map (_.flatten)

  def scheduled(nb: Int): Fu[List[Winner]] = scheduledCache apply nb
}
