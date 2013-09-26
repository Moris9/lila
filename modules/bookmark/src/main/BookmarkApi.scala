package lila.bookmark

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo }
import lila.user.User
import tube.bookmarkTube

final class BookmarkApi(
    cached: Cached,
    paginator: PaginatorBuilder) {

  def toggle(gameId: String, userId: String): Funit =
    $find.byId[Game](gameId) flatMap { gameOption ⇒
      gameOption ?? { game ⇒
        BookmarkRepo.toggle(gameId, userId) flatMap { bookmarked ⇒
          GameRepo.incBookmarks(gameId, bookmarked.fold(1, -1)) >>
            (cached.gameIds remove userId)
        }
      }
    }

  def bookmarked(game: Game, user: User): Fu[Boolean] = cached.bookmarked(game.id, user.id)

  def countByUser(user: User): Fu[Int] = cached.count(user.id)

  def userIdsByGame(game: Game): Fu[List[String]] =
    game.hasBookmarks ?? (BookmarkRepo userIdsByGameId game.id)

  def removeByGameIds(ids: List[String]): Funit = BookmarkRepo removeByGameIds ids

  def gamePaginatorByUser(user: User, page: Int) =
    paginator.byUser(user, page) map2 { (b: Bookmark) ⇒ b.game }
}
