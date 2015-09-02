package lila.app
package mashup

import lila.db.api.SortOrder
import lila.game.{ Game, Query }
import lila.user.User

import play.api.libs.json._
import scalaz.NonEmptyList

sealed abstract class GameFilter(val name: String)

object GameFilter {
  case object All extends GameFilter("all")
  case object Me extends GameFilter("me")
  case object Rated extends GameFilter("rated")
  case object Win extends GameFilter("win")
  case object Loss extends GameFilter("loss")
  case object Draw extends GameFilter("draw")
  case object Playing extends GameFilter("playing")
  case object Bookmark extends GameFilter("bookmark")
  case object Imported extends GameFilter("import")
}

case class GameFilterMenu(
    all: NonEmptyList[GameFilter],
    current: GameFilter) {

  def list = all.list
}

object GameFilterMenu {

  import GameFilter._
  import lila.db.Implicits.docId

  val all = NonEmptyList.nel(All, List(Me, Rated, Win, Loss, Draw, Playing, Bookmark, Imported))

  def apply(
    info: UserInfo,
    me: Option[User],
    currentNameOption: Option[String]): GameFilterMenu = {

    val user = info.user

    val filters = NonEmptyList.nel(All, List(
      (info.nbWithMe > 0) option Me,
      (info.nbRated > 0) option Rated,
      (info.user.count.win > 0) option Win,
      (info.user.count.loss > 0) option Loss,
      (info.user.count.draw > 0) option Draw,
      (info.nbPlaying > 0) option Playing,
      (info.nbBookmark > 0) option Bookmark,
      (info.nbImported > 0) option Imported
    ).flatten)

    val currentName = currentNameOption | info.hasSimul.fold(
      Playing,
      if (!info.user.hasGames && info.nbImported > 0) Imported else All
    ).name

    val current = currentOf(filters, currentName)

    new GameFilterMenu(filters, current)
  }

  def currentOf(filters: NonEmptyList[GameFilter], name: String) =
    (filters.list find (_.name == name)) | filters.head

  private def cachedNbOf(
    user: User,
    info: Option[UserInfo],
    filter: GameFilter): Option[Int] = filter match {
    case Bookmark => info.map(_.nbBookmark)
    case Imported => info.map(_.nbImported)
    case All      => user.count.game.some
    case Rated    => user.count.rated.some
    case Win      => user.count.win.some
    case Loss     => user.count.loss.some
    case Draw     => user.count.draw.some
    case _        => None
  }

  import lila.common.paginator._
  private def pag = Env.game.paginator
  def paginatorOf(
    user: User,
    info: Option[UserInfo],
    filter: GameFilter,
    me: Option[User],
    page: Int): Fu[Paginator[Game]] = {
    val nb = cachedNbOf(user, info, filter)
    def std(query: JsObject) = pag.recentlyCreated(query, nb)(page)
    filter match {
      case Bookmark => Env.bookmark.api.gamePaginatorByUser(user, page)
      case Imported => pag.apply(
        selector = Query imported user.id,
        sort = Seq("pgni.ca" -> SortOrder.Descending),
        nb = nb)(page)
      case All   => std(Query started user)
      case Me    => std(Query.opponents(user, me | user))
      case Rated => std(Query rated user)
      case Win   => std(Query win user)
      case Loss  => std(Query loss user)
      case Draw  => std(Query draw user)
      case Playing => pag.apply(
        selector = Query nowPlaying user.id,
        sort = Seq(),
        nb = nb)(page)
    }
  }
}
