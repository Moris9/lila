package lila.puzzle

import reactivemongo.api.bson.BSONRegex
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.chaining._

import lila.db.dsl._
import lila.memo.CacheApi
import lila.rating.{ Perf, PerfType }
import lila.user.{ User, UserRepo }

private case class PuzzleSession(
    theme: PuzzleTheme.Key,
    tier: PuzzleTier,
    path: Puzzle.PathId,
    positionInPath: Int,
    previousPaths: Set[Puzzle.PathId] = Set.empty,
    previousVotes: List[Boolean] = List.empty // most recent first
) {
  def switchTo(tier: PuzzleTier, pathId: Puzzle.PathId) = copy(
    tier = tier,
    path = pathId,
    previousPaths = previousPaths + pathId,
    positionInPath = 0
  )
  def next = copy(positionInPath = positionInPath + 1)
}

final class PuzzleSessionApi(colls: PuzzleColls, cacheApi: CacheApi, userRepo: UserRepo)(implicit
    ec: ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

  import BsonHandlers._
  import Puzzle.PathId

  sealed private trait NextPuzzleResult
  private object NextPuzzleResult {
    case object PathMissing                        extends NextPuzzleResult
    case object PathEnded                          extends NextPuzzleResult
    case class PuzzleMissing(id: Puzzle.Id)        extends NextPuzzleResult
    case class PuzzleAlreadyPlayed(puzzle: Puzzle) extends NextPuzzleResult
    case class PuzzleFound(puzzle: Puzzle)         extends NextPuzzleResult
  }

  def nextPuzzleFor(user: User, theme: PuzzleTheme.Key, retries: Int = 0): Fu[Puzzle] =
    continueOrCreateSessionFor(user, theme) flatMap { session =>
      import NextPuzzleResult._
      def switchPath(tier: PuzzleTier) =
        nextPathIdFor(user.id, theme, tier, session.previousPaths) flatMap {
          case None => fufail(s"No remaining puzzle path for ${user.id}")
          case Some(pathId) =>
            val newSession = session.switchTo(tier, pathId)
            sessions.put(user.id, fuccess(newSession))
            nextPuzzleFor(user, theme, retries = retries + 1)
        }

      nextPuzzleResult(user, session).thenPp(session.path.value) flatMap {
        case PathMissing | PathEnded if retries < 10 => switchPath(session.tier)
        case PathMissing | PathEnded                 => fufail(s"Puzzle path missing or ended for ${user.id}")
        case PuzzleMissing(id) =>
          logger.warn(s"Puzzle missing: $id")
          sessions.put(user.id, fuccess(session.next))
          nextPuzzleFor(user, theme, retries)
        case PuzzleAlreadyPlayed(_) if retries < 3 =>
          sessions.put(user.id, fuccess(session.next))
          nextPuzzleFor(user, theme, retries = retries + 1)
        case PuzzleAlreadyPlayed(_) if session.tier == PuzzleTier.Top => switchPath(PuzzleTier.All)
        case PuzzleAlreadyPlayed(puzzle)                              => fuccess(puzzle)
        case PuzzleFound(puzzle)                                      => fuccess(puzzle)
      }
    }

  private def nextPuzzleResult(user: User, session: PuzzleSession): Fu[NextPuzzleResult] =
    colls.path {
      _.aggregateOne() { framework =>
        import framework._
        Match($id(session.path)) -> List(
          // get the puzzle ID from session position
          Project($doc("puzzleId" -> $doc("$arrayElemAt" -> $arr("$ids", session.positionInPath)))),
          Project(
            $doc(
              "puzzleId" -> true,
              "roundId"  -> $doc("$concat" -> $arr(s"${user.id}${PuzzleRound.idSep}", "$puzzleId"))
            )
          ),
          // fetch the puzzle
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from"         -> colls.puzzle.name.value,
                "localField"   -> "puzzleId",
                "foreignField" -> "_id",
                "as"           -> "puzzle"
              )
            )
          ),
          // look for existing round
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from"         -> colls.round.name.value,
                "localField"   -> "roundId",
                "foreignField" -> "_id",
                "as"           -> "round"
              )
            )
          )
        )
      }.map { docOpt =>
        import NextPuzzleResult._
        // println(docOpt map lila.db.BSON.debug)
        docOpt.fold[NextPuzzleResult](PathMissing) { doc =>
          doc.getAsOpt[Puzzle.Id]("puzzleId").fold[NextPuzzleResult](PathEnded) { puzzleId =>
            doc
              .getAsOpt[List[Puzzle]]("puzzle")
              .flatMap(_.headOption)
              .fold[NextPuzzleResult](PuzzleMissing(puzzleId)) { puzzle =>
                if (doc.getAsOpt[List[Bdoc]]("round").exists(_.nonEmpty)) PuzzleAlreadyPlayed(puzzle)
                else PuzzleFound(puzzle)
              }
          }
        }
      }
    }

  def onComplete(round: PuzzleRound, theme: PuzzleTheme.Key): Funit =
    sessions.getIfPresent(round.userId) ?? {
      _ map { session =>
        // yes, even if the completed puzzle was not the current session puzzle
        // in that case we just skip a puzzle on the path, which doesn't matter
        if (session.theme == theme)
          sessions.put(round.userId, fuccess(session.next))
      }
    }

  private val sessions = cacheApi.notLoading[User.ID, PuzzleSession](32768, "puzzle.session")(
    _.expireAfterWrite(1 hour).buildAsync()
  )

  private[puzzle] def currentSessionOf(user: User, theme: PuzzleTheme.Key): Fu[PuzzleSession] =
    sessions.getFuture(user.id, _ => createSessionFor(user, theme))

  private[puzzle] def continueOrCreateSessionFor(
      user: User,
      theme: PuzzleTheme.Key
  ): Fu[PuzzleSession] =
    currentSessionOf(user, theme) flatMap { current =>
      if (current.theme == theme) fuccess(current)
      else createSessionFor(user, theme) tap { sessions.put(user.id, _) }
    }

  private def createSessionFor(user: User, theme: PuzzleTheme.Key): Fu[PuzzleSession] =
    nextPathIdFor(user.id, theme, PuzzleTier.Top, Set.empty)
      .orFail(s"No puzzle path found for ${user.id}, theme: $theme")
      .dmap(pathId => PuzzleSession(theme, PuzzleTier.Top, pathId, 0))

  private def nextPathIdFor(
      userId: User.ID,
      theme: PuzzleTheme.Key,
      tier: PuzzleTier,
      previousPaths: Set[PathId]
  ): Fu[Option[PathId]] =
    userRepo.perfOf(userId, PerfType.Puzzle).dmap(_ | Perf.default) flatMap { perf =>
      colls.path {
        _.aggregateOne() { framework =>
          import framework._
          Match(
            $doc(
              "_id" ->
                $doc(
                  "$regex" -> BSONRegex(s"^${theme}_${tier}", ""),
                  $nin(previousPaths)
                ),
              "min" $lte perf.glicko.rating,
              "max" $gt perf.glicko.rating
            )
          ) -> List(
            Project($id(true)),
            Sample(1)
          )
        }.dmap(_.flatMap(_.getAsOpt[PathId]("_id")))
      }
    }
}
