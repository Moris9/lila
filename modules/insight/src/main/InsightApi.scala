package lila.insight

import org.joda.time.DateTime
import reactivemongo.api.bson.BSONNull

import lila.db.dsl._
import lila.game.{ Game, GameRepo, Pov }
import lila.rating.PerfType
import lila.user.User

final class InsightApi(
    storage: InsightStorage,
    pipeline: AggregationPipeline,
    insightUserApi: InsightUserApi,
    gameRepo: GameRepo,
    indexer: InsightIndexer
)(implicit ec: scala.concurrent.ExecutionContext) {

  import InsightApi._

  def insightUser(user: User): Fu[InsightUser] =
    insightUserApi find user.id flatMap {
      case Some(u) =>
        u.lastSeen.isBefore(DateTime.now minusDays 1) ?? {
          insightUserApi setSeenNow user
        } inject u
      case None =>
        for {
          count                <- storage count user.id
          (families, openings) <- storage openings user.id
          c = InsightUser.make(user.id, count, families, openings)
          _ <- insightUserApi save c
        } yield c
    }

  def ask[X](question: Question[X], user: User, withPovs: Boolean = true): Fu[Answer[X]] =
    pipeline
      .aggregate(question, Left(user), withPovs = withPovs)
      .flatMap { aggDocs =>
        val clusters = AggregationClusters(question, aggDocs)
        withPovs ?? {
          val gameIds = lila.common.ThreadLocalRandom.shuffle(clusters.flatMap(_.gameIds)) take 4
          gameRepo.userPovsByGameIds(gameIds, user)
        } map { Answer(question, clusters, _) }
      }
      .monSuccess(_.insight.user)

  def askPeers[X](question: Question[X], rating: MeanRating): Fu[Answer[X]] =
    pipeline
      .aggregate(question, Right(Question.Peers(rating)), withPovs = false)
      .map { aggDocs =>
        Answer(question, AggregationClusters(question, aggDocs), Nil)
      }
      .monSuccess(_.insight.peers)

  def perfStats(user: User, perfTypes: List[PerfType]): Fu[Map[PerfType, PerfStats]] =
    storage.coll {
      _.aggregateList(perfTypes.size) { framework =>
        import framework._
        import InsightEntry.{ BSONFields => F }
        val filters = List(lila.insight.Filter(InsightDimension.Perf, perfTypes))
        Match(InsightStorage.selectUserId(user.id) ++ pipeline.gameMatcher(filters)) -> List(
          Sort(Descending(F.date)),
          Limit(pipeline.maxGames),
          GroupField(F.perf)("r" -> AvgField("mr"), "n" -> SumAll)
        )
      }.map { docs =>
        for {
          doc <- docs
          id  <- doc int "_id"
          pt  <- PerfType(id)
          ra  <- doc double "r"
          nb  <- doc int "n"
        } yield pt -> PerfStats(MeanRating(ra.toInt), nb)
      }.map(_.toMap)
    }

  def userStatus(user: User): Fu[UserStatus] =
    gameRepo lastFinishedRatedNotFromPosition user flatMap {
      case None => fuccess(UserStatus.NoGame)
      case Some(game) =>
        storage fetchLast user.id map {
          case None                                              => UserStatus.Empty
          case Some(entry) if entry.date isBefore game.createdAt => UserStatus.Stale
          case _                                                 => UserStatus.Fresh
        }
    }

  def indexAll(user: User) =
    indexer
      .all(user)
      .monSuccess(_.insight.index) >>
      insightUserApi.remove(user.id)

  def updateGame(g: Game) =
    Pov(g)
      .map { pov =>
        pov.player.userId ?? { userId =>
          storage find InsightEntry.povToId(pov) flatMap {
            _ ?? { indexer.update(g, userId, _) }
          }
        }
      }
      .sequenceFu
      .void
}

object InsightApi {

  sealed trait UserStatus
  object UserStatus {
    case object NoGame extends UserStatus // the user has no rated games
    case object Empty  extends UserStatus // insights not yet generated
    case object Stale  extends UserStatus // new games not yet generated
    case object Fresh  extends UserStatus // up to date
  }
}
