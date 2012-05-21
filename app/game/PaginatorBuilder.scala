package lila
package game

import user.User
import chess.Status

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports._

final class PaginatorBuilder(
    gameRepo: GameRepo,
    maxPerPage: Int) {

  def recent(page: Int): Paginator[DbGame] = 
    paginator(recentAdapter, page)

  def checkmate(page: Int): Paginator[DbGame] = 
    paginator(checkmateAdapter, page)

  def userAll(user: User, page: Int): Paginator[DbGame] =
    paginator(userAllAdapter(user), page)

  private val recentAdapter = 
    adapter(DBObject())

  private val checkmateAdapter = 
    adapter(DBObject("status" -> Status.Mate.id))

  private def userAllAdapter(user: User) =
    adapter(("status" $gte Status.Started.id) ++ ("userIds" -> user.id.toString))

  private def adapter(query: DBObject) = SalatAdapter(
    dao = gameRepo,
    query = query,
    sort = DBObject("updatedAt" -> -1)
  ) map (_.decode.get) // unsafe

  private def paginator(adapter: Adapter[DbGame], page: Int) = Paginator(
    adapter,
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ recent(0), identity)
}
