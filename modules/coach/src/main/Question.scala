package lila.coach

case class Question[X, Dim <: Dimension[X]](
  xAxis: Dim,
  yAxis: Metric,
  filters: List[Filter[_]])

case class Filter[A](
    dimension: Dimension[A],
    selected: List[A]) {

  import reactivemongo.bson._

  def matcher: BSONDocument = selected map dimension.bson.write match {
    case Nil     => BSONDocument()
    case List(x) => BSONDocument(dimension.dbKey -> x)
    case xs      => BSONDocument(dimension.dbKey -> BSONDocument("$or" -> BSONArray(xs)))
  }
}

case class Answer[X, Dim <: Dimension[X]](
  question: Question[X, Dim],
  clusters: List[Cluster[X]])

case class Cluster[X](
  x: X,
  points: List[Point])

case class Point(
  name: String,
  y: Double)
