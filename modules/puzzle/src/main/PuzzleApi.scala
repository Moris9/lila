package lila.puzzle

import cats.implicits._
import scala.concurrent.duration._

import lila.db.AsyncColl
import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.{ User, UserRepo }

final private[puzzle] class PuzzleApi(
    colls: PuzzleColls,
    pathApi: PuzzlePathApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Puzzle.{ BSONFields => F }
  import BsonHandlers._

  object puzzle {

    def find(id: Puzzle.Id): Fu[Option[Puzzle]] =
      colls.puzzle(_.byId[Puzzle](id.value))

    def delete(id: Puzzle.Id): Funit =
      colls.puzzle(_.delete.one($id(id.value))).void

    def count = colls.puzzle(_.countAll).dmap(_.toInt)
  }

  object round {

    def find(user: User, puzzle: Puzzle): Fu[Option[PuzzleRound]] =
      colls.round(_.byId[PuzzleRound](PuzzleRound.Id(user.id, puzzle.id).toString))

    def upsert(a: PuzzleRound) = colls.round(_.update.one($id(a.id), a, upsert = true))

    def addDenormalizedUser(a: PuzzleRound, user: User): Funit = colls.round(
      _.updateField($id(a.id), PuzzleRound.BSONFields.user, user.id).void
    )
  }

  object vote {

    def update(id: Puzzle.Id, user: User, vote: Option[Boolean]): Funit =
      colls.round {
        _.ext
          .findAndUpdate[PuzzleRound](
            $id(PuzzleRound.Id(user.id, id)),
            vote match {
              case None    => $unset(PuzzleRound.BSONFields.vote)
              case Some(v) => $set($doc(PuzzleRound.BSONFields.vote -> v))
            }
          )
      } flatMap {
        case Some(prevRound) if prevRound.vote != vote =>
          def voteToInt(v: Option[Boolean]) = v ?? { w => if (w) 1 else -1 }
          colls.puzzle {
            _.incField($id(id), F.vote, voteToInt(vote) - voteToInt(prevRound.vote)).void
          }
        case _ => funit
      }
  }

  object theme {

    def sortedWithCount: Fu[List[PuzzleTheme.WithCount]] =
      colls.path {
        _.aggregateList(Int.MaxValue) { framework =>
          import framework._
          Match($doc("tier" -> "all")) -> List(
            GroupField("tag")(
              "count" -> SumField("length")
            )
          )
        }.map { objs =>
          val byKey = objs.flatMap { obj =>
            for {
              key   <- obj string "_id" map PuzzleTheme.Key
              count <- obj int "count"
              theme <- PuzzleTheme.byKey get key
            } yield key -> PuzzleTheme.WithCount(theme, count)
          }.toMap
          PuzzleTheme.sorted.flatMap(pt => byKey.get(pt.key))
        }
      }
  }
}
