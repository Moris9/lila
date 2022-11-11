package lila.swiss

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import com.softwaremill.tagging.*
import scala.concurrent.duration.*

import lila.common.Heapsort
import lila.db.dsl.{ *, given }
import lila.hub.LightTeam.TeamID
import lila.memo.CacheApi
import lila.memo.CacheApi.*

final class SwissFeature(
    swissColl: Coll @@ SwissColl,
    cacheApi: CacheApi,
    swissCache: SwissCache
)(using ec: scala.concurrent.ExecutionContext):

  import BsonHandlers.given

  val onHomepage = cacheApi.unit[Option[Swiss]] {
    _.refreshAfterWrite(1 minute)
      .buildAsyncFuture { _ =>
        swissColl
          .find($doc("teamId" -> lichessTeamId, "startsAt" $gt DateTime.now.minusMinutes(10)))
          .sort($sort asc "startsAt")
          .one[Swiss]
      }
  }

  def get(teams: Seq[TeamID]) =
    cache.getUnit zip getForTeams(teams :+ lichessTeamId distinct) map { case (cached, teamed) =>
      FeaturedSwisses(
        created = (teamed.created ::: cached.created).distinctBy(_.id),
        started = (teamed.started ::: cached.started).distinctBy(_.id)
      )
    }

  private val startsAtOrdering = Ordering.by[Swiss, Long](_.startsAt.getMillis)

  private def getForTeams(teams: Seq[TeamID]): Fu[FeaturedSwisses] =
    teams.map(swissCache.featuredInTeam.get).sequenceFu.dmap(_.flatten) flatMap { ids =>
      swissColl.byStringIds[Swiss](ids.map(_.value), ReadPreference.secondaryPreferred)
    } map {
      _.filter(_.isNotFinished).partition(_.isCreated) match
        case (created, started) =>
          FeaturedSwisses(
            created = Heapsort.topN(created, 10)(using startsAtOrdering.reverse),
            started = Heapsort.topN(started, 10)(using startsAtOrdering)
          )
    }

  private val cache = cacheApi.unit[FeaturedSwisses] {
    _.refreshAfterWrite(10 seconds)
      .buildAsyncFuture { _ =>
        val now = DateTime.now
        cacheCompute($doc("$gt" -> now, "$lt" -> now.plusHours(1))) zip
          cacheCompute($doc("$gt" -> now.minusHours(3), "$lt" -> now)) map { case (created, started) =>
            FeaturedSwisses(created, started)
          }
      }
  }

  // causes heavy team reads
  private def cacheCompute(startsAtRange: Bdoc, nb: Int = 5): Fu[List[Swiss]] =
    swissColl
      .aggregateList(nb, ReadPreference.secondaryPreferred) { framework =>
        import framework.*
        Match(
          $doc(
            "featurable" -> true,
            "settings.i" $lte 600, // hits the partial index
            "startsAt" -> startsAtRange,
            "garbage" $ne true
          )
        ) -> List(
          Sort(Descending("nbPlayers")),
          Limit(nb * 50),
          PipelineOperator(
            $lookup.pipeline(
              from = "team",
              as = "team",
              local = "teamId",
              foreign = "_id",
              pipe = List(
                $doc("$match"   -> $doc("open" -> true, "password" $exists false)),
                $doc("$project" -> $id(true))
              )
            )
          ),
          UnwindField("team"),
          Limit(nb)
        )
      }
      .map { _.flatMap(_.asOpt[Swiss]) }
