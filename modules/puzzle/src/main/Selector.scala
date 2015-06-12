package lila.puzzle

import scala.concurrent.duration._
import scala.util.Random

import reactivemongo.api.QueryOpts
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONArray }
import reactivemongo.core.commands.Count

import lila.db.Types.Coll
import lila.user.User

private[puzzle] final class Selector(
    puzzleColl: Coll,
    api: PuzzleApi,
    anonMinRating: Int,
    toleranceStep: Int,
    toleranceMax: Int,
    maxAttempts: Int) {

  private def popularSelector(mate: Boolean) = BSONDocument(
    Puzzle.BSONFields.voteSum -> BSONDocument("$gt" -> BSONInteger(mate.fold(anonMinRating, 0))))

  private def mateSelector(mate: Boolean) = BSONDocument("mate" -> mate)

  private def difficultyDecay(difficulty: Int) = difficulty match {
    case 1 => -200
    case 3 => +200
    case _ => 0
  }

  val anonSkipMax = 2000

  def apply(me: Option[User], difficulty: Int): Fu[Option[Puzzle]] = {
    val isMate = scala.util.Random.nextBoolean
    me match {
      case None =>
        puzzleColl.find(popularSelector(isMate) ++ mateSelector(isMate))
          .options(QueryOpts(skipN = Random nextInt anonSkipMax))
          .one[Puzzle]
      case Some(user) if user.perfs.puzzle.nb > maxAttempts => fuccess(none)
      case Some(user) => api.attempt.playedIds(user, maxAttempts) flatMap { ids =>
        tryRange(user, toleranceStep, difficultyDecay(difficulty), ids, isMate)
      }
    }
  }

  private def tryRange(user: User, tolerance: Int, decay: Int, ids: BSONArray, isMate: Boolean): Fu[Option[Puzzle]] =
    puzzleColl.find(mateSelector(isMate) ++ BSONDocument(
      Puzzle.BSONFields.id -> BSONDocument("$nin" -> ids),
      Puzzle.BSONFields.rating -> BSONDocument(
        "$gt" -> BSONInteger(user.perfs.puzzle.intRating - tolerance + decay),
        "$lt" -> BSONInteger(user.perfs.puzzle.intRating + tolerance + decay)
      )
    )).sort(BSONDocument(Puzzle.BSONFields.voteSum -> -1))
      .one[Puzzle] flatMap {
        case None if (tolerance + toleranceStep) <= toleranceMax =>
          tryRange(user, tolerance + toleranceStep, decay, ids, isMate)
        case res => fuccess(res)
      }
}
