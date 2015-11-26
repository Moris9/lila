package lila.coach

import reactivemongo.bson._

import lila.db.Implicits._
import lila.user.User

final class CoachApi(coll: Coll) {

  import Storage._
  import coll.BatchCommands.AggregationFramework._
  import lila.coach.{ Dimension => D, Metric => M }

  def ask[X](
    question: Question[X],
    user: User): Fu[Answer[X]] = {
    println(question)
    val gameMatcher = combineDocs(question.filters.collect {
      case f if f.dimension.isInGame => f.matcher
    })
    println(gameMatcher)
    val moveMatcher = combineDocs(question.filters.collect {
      case f if f.dimension.isInMove => f.matcher
    }).some.filterNot(_.isEmpty) map Match
    println(moveMatcher)
    coll.aggregate(
      Match(selectUserId(user.id) ++ gameMatcher),
      makePipeline(question.dimension, question.metric, moveMatcher).flatten
    ).map { res =>
        val clusters = res.documents.flatMap { doc =>
          for {
            id <- doc.getAs[X]("_id")(question.dimension.bson)
            value <- doc.getAs[BSONNumberLike]("v")
            nb <- doc.getAs[Int]("n")
          } yield Cluster(id,
            Point.Data(question.metric.name, value.toDouble),
            Point.Size(question.metric.position.tellNumber, nb))
        }
        Answer(
          question,
          clusters |> postProcess(question) |> postSort(question)
        )
      }
  }

  private val unwindMoves = Unwind("moves").some
  private val sortNb = Sort(Descending("nb")).some
  private def limit(nb: Int) = Limit(nb).some
  private def group(d: Dimension[_], f: GroupFunction) = GroupField(d.dbKey)(
    "v" -> f,
    "n" -> SumValue(1)
  ).some

  private def makePipeline(
    dimension: Dimension[_],
    metric: Metric,
    moveMatcher: Option[Match]): List[Option[PipelineOperator]] = (metric match {
    case M.MeanCpl => List(
      unwindMoves,
      moveMatcher,
      group(dimension, Avg("moves.c"))
    )
    case M.NbMoves => List(
      unwindMoves,
      moveMatcher,
      group(dimension, SumValue(1))
    )
    case M.Movetime => List(
      unwindMoves,
      moveMatcher,
      group(dimension, GroupFunction("$avg", BSONDocument("$divide" -> BSONArray("$moves.t", 10))))
    )
    case M.RatingDiff => List(
      group(dimension, Avg("ratingDiff"))
    )
    // case M.Result => List(
    //   GroupField(x.dbKey)(
    //     "nb" -> SumValue(1),
    //     "v" -> Avg("moves.c")
    //   ).some
    // )
    case _ => Nil
  }) ::: (dimension match {
    case D.Opening => List(sortNb, limit(12))
    case _         => Nil
  })

  private def postProcess[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] =
    clusters

  private def postSort[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] = q.dimension match {
    case D.Opening => clusters
    case _         => sortLike[Cluster[X], X](clusters, D.valuesOf(q.dimension), _.x)
  }

  private def sortLike[A, B](la: List[A], lb: List[B], f: A => B): List[A] = la.sortWith {
    case (x, y) => lb.indexOf(f(x)) < lb.indexOf(f(y))
  }
}
