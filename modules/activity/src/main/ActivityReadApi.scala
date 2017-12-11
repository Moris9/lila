package lila.activity

import org.joda.time.{ DateTime, Interval }

import lila.db.dsl._
import lila.game.{ Pov, GameRepo }
import lila.practice.PracticeStructure
import lila.user.User

final class ActivityReadApi(
    coll: Coll,
    practiceApi: lila.practice.PracticeApi,
    postApi: lila.forum.PostApi,
    simulApi: lila.simul.SimulApi,
    studyApi: lila.study.StudyApi,
    tourLeaderApi: lila.tournament.LeaderboardApi
) {

  import Activity._
  import BSONHandlers._
  import model._

  private val recentNb = 7

  def recent(u: User): Fu[Vector[ActivityView]] = for {
    allActivities <- coll.find(regexId(u.id))
      .sort($sort desc "_id")
      .gather[Activity, Vector](recentNb)
    activities = allActivities.filterNot(_.isEmpty)
    practiceStructure <- activities.exists(_.practice.isDefined) ?? {
      practiceApi.structure.get map some
    }
    views <- activities.map { one(u, practiceStructure) _ }.sequenceFu
  } yield addSignup(u.createdAt, views)

  private def one(u: User, practiceStructure: Option[PracticeStructure])(a: Activity): Fu[ActivityView] = for {
    posts <- a.posts ?? { p =>
      postApi.liteViewsByIds(p.value.map(_.value)) dmap some
    }
    practice = (for {
      p <- a.practice
      struct <- practiceStructure
    } yield p.value flatMap {
      case (studyId, nb) => struct study studyId map (_ -> nb)
    } toMap)
    postView = posts.map { p =>
      p.groupBy(_.topic).mapValues { posts =>
        posts.map(_.post).sortBy(_.createdAt)
      }
    } filter (_.nonEmpty)
    corresMoves <- a.corres ?? { corres =>
      getPovs(a.id.userId, corres.movesIn) dmap {
        _.map(corres.moves -> _)
      }
    }
    corresEnds <- a.corres ?? { corres =>
      getPovs(a.id.userId, corres.end) dmap {
        _.map { povs =>
          Score.make(povs) -> povs
        }
      }
    }
    simuls <- a.simuls.?? { simuls =>
      simulApi byIds simuls.value.map(_.value) dmap some
    }.map(_ filter (_.nonEmpty))
    studies <- a.studies.?? { studies =>
      studyApi publicIdNames studies.value map some
    }.map(_ filter (_.nonEmpty))
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
    tours = tours
  )

  private def addSignup(at: DateTime, recent: Vector[ActivityView]) = {
    val (found, views) = recent.foldLeft(false -> Vector.empty[ActivityView]) {
      case ((false, as), a) if a.interval contains at => (true, as :+ a.copy(signup = true))
      case ((found, as), a) => (found, as :+ a)
    }
    if (!found && views.size < recentNb && DateTime.now.minusDays(8).isBefore(at))
      views :+ ActivityView(
        interval = new Interval(at.withTimeAtStartOfDay, at.withTimeAtStartOfDay plusDays 1),
        signup = true
      )
    else views
  }

  private def getPovs(userId: User.ID, gameIds: List[GameId]): Fu[Option[List[Pov]]] = gameIds.nonEmpty ?? {
    GameRepo.gamesFromSecondary(gameIds.map(_.value)).dmap {
      _.flatMap {
        Pov.ofUserId(_, userId)
      }.some.filter(_.nonEmpty)
    }
  }

  private def makeIds(userId: User.ID, days: Int): List[Id] =
    Day.recent(days).map { Id(userId, _) }
}
