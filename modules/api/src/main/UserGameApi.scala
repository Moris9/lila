package lila.api

import play.api.libs.json._

import chess.format.Forsyth
import lila.common.LightUser
import lila.common.paginator.Paginator
import lila.common.PimpedJson._
import lila.game.{ Game, PerfPicker }

final class UserGameApi(
    bookmarkApi: lila.bookmark.BookmarkApi,
    lightUser: LightUser.Getter) {

  import lila.game.JsonView._
  import lila.round.JsonView._
  import LightUser.lightUserWrites

  def jsPaginator(pag: Paginator[Game])(implicit ctx: Context): Fu[JsObject] =
    bookmarkApi.filterGameIdsBookmarkedBy(pag.currentPageResults, ctx.me) map { bookmarkedIds =>
      implicit val gameWriter = Writes[Game] { g =>
        write(g, bookmarkedIds(g.id))
      }
      Json.obj(
        "paginator" -> lila.common.paginator.PaginatorJson(pag)
      )
    }

  private def write(g: Game, bookmarked: Boolean) = Json.obj(
    "id" -> g.id,
    "rated" -> g.rated,
    "variant" -> g.variant,
    "speed" -> g.speed.key,
    "perf" -> PerfPicker.key(g),
    "timestamp" -> g.createdAt.getDate,
    "turns" -> g.turns,
    "status" -> g.status,
    "clock" -> g.clock,
    "correspondence" -> g.daysPerTurn.map { d =>
      Json.obj("daysPerTurn" -> d)
    },
    "source" -> g.source.map(_.name),
    "players" -> JsObject(g.players map { p =>
      p.color.name -> Json.obj(
        "user" -> p.userId.flatMap(lightUser),
        "userId" -> p.userId, // for BC
        "name" -> p.name,
        "aiLevel" -> p.aiLevel,
        "rating" -> p.rating,
        "ratingDiff" -> p.ratingDiff
      ).noNull
    }),
    "fen" -> Forsyth.exportBoard(g.toChess.board),
    "lastMove" -> g.castleLastMoveTime.lastMoveString,
    "opening" -> g.opening,
    "winner" -> g.winnerColor.map(_.name),
    "bookmarks" -> g.bookmarks,
    "bookmarked" -> bookmarked.option(true),
    "analysed" -> g.metadata.analysed.option(true)
  ).noNull
}
