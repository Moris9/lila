package lila.puzzle

import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import org.joda.time.DateTime
import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex }
import reactivemongo.core.commands.Count

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private[puzzle] final class PuzzleApi(
    puzzleColl: Coll,
    attemptColl: Coll,
    apiToken: String) {

  import Puzzle.puzzleBSONHandler

  object puzzle {

    def find(id: PuzzleId): Fu[Option[Puzzle]] =
      puzzleColl.find(BSONDocument("_id" -> id))
        .projection(Puzzle.withoutUsers)
        .one[Puzzle]

    def latest(nb: Int): Fu[List[Puzzle]] =
      puzzleColl.find(BSONDocument())
        .projection(Puzzle.withoutUsers)
        .sort(BSONDocument("date" -> -1))
        .cursor[Puzzle]
        .collect[List](nb)

    def importBatch(json: JsValue, token: String): Fu[List[Try[PuzzleId]]] =
      if (token != apiToken) fufail("Invalid API token")
      else {
        import Generated.generatedJSONRead
        insertPuzzles(json.as[List[Generated]] map (_.toPuzzle))
      }

    def insertPuzzles(puzzles: List[Try[PuzzleId => Puzzle]]): Fu[List[Try[PuzzleId]]] = puzzles match {
      case Nil => fuccess(Nil)
      case Failure(err) :: rest => insertPuzzles(rest) map { ps =>
        (Failure(err): Try[PuzzleId]) :: ps
      }
      case Success(puzzle) :: rest => findNextId flatMap { id =>
        val p = puzzle(id)
        val fenStart = p.fen.split(' ').take(2).mkString(" ")
        puzzleColl.db command Count(puzzleColl.name, BSONDocument(
          "fen" -> BSONRegex(fenStart.replace("/", "\\/"), "")
        ).some) flatMap {
          case 0 => (puzzleColl insert p) >> {
            insertPuzzles(rest) map (Success(id) :: _)
          }
          case _ => insertPuzzles(rest) map (Failure(new Exception("Duplicate puzzle")) :: _)
        }
      }
    }

    private def findNextId: Fu[PuzzleId] =
      puzzleColl.find(BSONDocument(), BSONDocument("_id" -> true))
        .sort(BSONDocument("_id" -> -1))
        .one[BSONDocument] map {
          _ flatMap { doc => doc.getAs[Int]("_id") map (1+) } getOrElse 1
        }
  }

  object attempt {

    def find(puzzleId: PuzzleId, userId: String): Fu[Option[Attempt]] =
      attemptColl.find(BSONDocument(
        Attempt.BSONFields.id -> Attempt.makeId(puzzleId, userId)
      )).one[Attempt]

    def vote(a1: Attempt, v: Boolean): Fu[(Puzzle, Attempt)] = puzzle find a1.puzzleId flatMap {
      case None => fufail(s"Can't vote for non existing puzzle ${a1.puzzleId}")
      case Some(p1) =>
        val p2 = a1.vote match {
          case Some(from) => p1 withVote (_.change(from, v))
          case None       => p1 withVote (_ add v)
        }
        val a2 = a1.copy(vote = v.some)
        attemptColl.update(
          BSONDocument("_id" -> a2.id),
          BSONDocument("$set" -> BSONDocument(Attempt.BSONFields.vote -> v))) zip
          puzzleColl.update(
            BSONDocument("_id" -> p2.id),
            BSONDocument("$set" -> BSONDocument(Puzzle.BSONFields.vote -> p2.vote))) map {
              case _ => p2 -> a2
            }
    }

    def add(a: Attempt) = attemptColl insert a void

    def hasPlayed(user: User, puzzle: Puzzle): Fu[Boolean] =
      attemptColl.db command Count(attemptColl.name, BSONDocument(
        Attempt.BSONFields.id -> Attempt.makeId(puzzle.id, user.id)
      ).some) map (0!=)

    def hasVoted(user: User): Fu[Boolean] = attemptColl.find(
      BSONDocument(Attempt.BSONFields.userId -> user.id),
      BSONDocument(
        Attempt.BSONFields.vote -> true,
        Attempt.BSONFields.id -> false
      )).sort(BSONDocument(Attempt.BSONFields.date -> -1))
      .cursor[BSONDocument]
      .collect[List](5) map {
        case attempts if attempts.size < 5 => true
        case attempts => attempts.foldLeft(false) {
          case (true, _)    => true
          case (false, doc) => doc.getAs[Boolean](Attempt.BSONFields.vote).isDefined
        }
      }
  }
}
