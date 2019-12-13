package lila.activity

import org.joda.time.{ DateTime, Interval }

import lila.db.dsl._
import lila.game.LightPov
import lila.practice.PracticeStructure
import lila.user.User

final class ActivityReadApi(
    coll: Coll,
    gameRepo: lila.game.GameRepo,
    practiceApi: lila.practice.PracticeApi,
    postApi: lila.forum.PostApi,
    simulApi: lila.simul.SimulApi,
    studyApi: lila.study.StudyApi,
    tourLeaderApi: lila.tournament.LeaderboardApi
) {

  import BSONHandlers._
  import model._

  implicit private val ordering = scala.math.Ordering.Double.TotalOrdering

  private val recentNb = 7

  def recent(u: User, nb: Int = recentNb): Fu[Vector[ActivityView]] =
    for {
      allActivities <- coll.ext
        .find(regexId(u.id))
        .sort($sort desc "_id")
        .gather[Activity, Vector](nb)
      activities = allActivities.filterNot(_.isEmpty)
      practiceStructure <- activities.exists(_.practice.isDefined) ?? {
        practiceApi.structure.get map some
      }
      views <- activities.map { one(practiceStructure) _ }.sequenceFu
    } yield addSignup(u.createdAt, views)

  private def one(practiceStructure: Option[PracticeStructure])(a: Activity): Fu[ActivityView] =
    for {
      posts <- a.posts ?? { p =>
        postApi.liteViewsByIds(p.value.map(_.value)) dmap some
      }
      practice = (for {
        p      <- a.practice
        struct <- practiceStructure
      } yield p.value flatMap {
        case (studyId, nb) => struct study studyId map (_ -> nb)
      } toMap)
      postView = posts.map { p =>
        p.groupBy(_.topic)
          .view
          .mapValues { posts =>
            posts.view.map(_.post).sortBy(_.createdAt).toList
          }
          .toMap
      } filter (_.nonEmpty)
      corresMoves <- a.corres ?? { corres =>
        getLightPovs(a.id.userId, corres.movesIn) dmap {
          _.map(corres.moves -> _)
        }
      }
      corresEnds <- a.corres ?? { corres =>
        getLightPovs(a.id.userId, corres.end) dmap {
          _.map { povs =>
            Score.make(povs) -> povs
          }
        }
      }
      simuls <- a.simuls
        .?? { simuls =>
          simulApi byIds simuls.value.map(_.value) dmap some
        }
        .map(_ filter (_.nonEmpty))
      studies <- a.studies
        .?? { studies =>
          studyApi publicIdNames studies.value map some
        }
        .map(_ filter (_.nonEmpty))
      tours <- a.games.exists(_.hasNonCorres) ?? {
        val dateRange = a.date -> a.date.plusDays(1)
        tourLeaderApi.timeRange(a.id.userId, dateRange) map { entries =>
          entries.nonEmpty option ActivityView.Tours(
            nb = entries.size,
            best = entries.sortBy(_.rankRatio.value).take(activities.maxSubEntries)
          )
        }
      }
    } yield ActivityView(
      interval = a.interval,
      games = a.games,
      puzzles = a.puzzles,
      practice = practice,
      posts = postView,
      simuls = simuls,
      patron = a.patron,
      corresMoves = corresMoves,
      corresEnds = corresEnds,
      follows = a.follows,
      studies = studies,
      teams = a.teams,
      tours = tours,
      stream = a.stream
    )

  private def addSignup(at: DateTime, recent: Vector[ActivityView]) = {
    val (found, views) = recent.foldLeft(false -> Vector.empty[ActivityView]) {
      case ((false, as), a) if a.interval contains at => (true, as :+ a.copy(signup = true))
      case ((found, as), a)                           => (found, as :+ a)
    }
    if (!found && views.size < recentNb && DateTime.now.minusDays(8).isBefore(at))
      views :+ ActivityView(
        interval = new Interval(at.withTimeAtStartOfDay, at.withTimeAtStartOfDay plusDays 1),
        signup = true
      )
    else views
  }

  private def getLightPovs(userId: User.ID, gameIds: List[GameId]): Fu[Option[List[LightPov]]] =
    gameIds.nonEmpty ?? {
      gameRepo.light.gamesFromSecondary(gameIds.map(_.value)).dmap {
        _.flatMap { LightPov.ofUserId(_, userId) }.some.filter(_.nonEmpty)
      }
    }
}
