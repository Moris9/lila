package lila.coach

import lila.common.paginator.Paginator

final class CoachPager(api: CoachApi) {

  val maxPerPage = 10

  import CoachPager._

  def apply(order: Order, page: Int): Fu[Paginator[Coach.WithUser]] =
    api.listedWithUserList.map { all =>
      Paginator.fromList(
        list = all sortWith order.predicate,
        currentPage = page,
        maxPerPage = maxPerPage
      )
    }
}

object CoachPager {

  sealed abstract class Order(
    val key: String,
    val name: String,
    val predicate: (Coach.WithUser, Coach.WithUser) => Boolean
  )

  object Order {
    case object Login extends Order("login", "Last login",
      (a, b) => a.user.timeNoSee < b.user.timeNoSee)
    case object LichessRating extends Order("rating", "Lichess rating",
      (a, b) => a.user.perfs.bestStandardRating > b.user.perfs.bestStandardRating)
    case object NbReview extends Order("review", "User reviews",
      (a, b) => a.coach.nbReviews > b.coach.nbReviews)
    case object Alphabetical extends Order("alphabetical", "Alphabetical",
      (a, b) => a.user.username < b.user.username)

    val default = Login
    val all = List(Login, LichessRating, NbReview, Alphabetical)
    def apply(key: String): Order = all.find(_.key == key) | default
  }
}
