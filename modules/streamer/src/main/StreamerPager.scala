package lila.streamer

import reactivemongo.api._

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.{ Adapter }
import lila.user.{ User, UserRepo }

final class StreamerPager(
    coll: Coll,
    maxPerPage: lila.common.MaxPerPage
) {

  import BsonHandlers._

  def apply(page: Int): Fu[Paginator[Streamer.WithUser]] = {
    val adapter = new Adapter[Streamer](
      collection = coll,
      selector = $doc("listed" -> Streamer.Listed(true)),
      projection = $empty,
      sort = $doc(
        "sorting.streaming" -> -1,
        "sorting.onlineAt" -> -1
      )
    ) mapFutureList withUsers
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = maxPerPage.value
    )
  }

  private def withUsers(streamers: Seq[Streamer]): Fu[Seq[Streamer.WithUser]] =
    UserRepo.withColl {
      _.optionsByOrderedIds[User, User.ID](streamers.map(_.id.value), ReadPreference.secondaryPreferred)(_.id)
    } map { users =>
      streamers zip users collect {
        case (streamer, Some(user)) => Streamer.WithUser(streamer, user)
      }
    }
}
