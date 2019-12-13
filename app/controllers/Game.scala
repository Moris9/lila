package controllers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.mvc._

import lila.api.GameApiV2
import lila.app._
import lila.common.config.MaxPerSecond
import lila.common.HTTPRequest
import lila.game.{ Game => GameModel }

final class Game(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  def delete(gameId: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.game.gameRepo game gameId) { game =>
      if (game.pgnImport.flatMap(_.user) ?? (me.id ==)) {
        env.hub.bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
        (env.game.gameRepo remove game.id) >>
          (env.analyse.analysisRepo remove game.id) >>
          env.game.cached.clearNbImportedByCache(me.id) inject
          Redirect(routes.User.show(me.username))
      } else
        fuccess {
          Redirect(routes.Round.watcher(game.id, game.firstColor.name))
        }
    }
  }

  def exportOne(id: String) = Open { implicit ctx =>
    OptionFuResult(env.game.gameRepo game id) { game =>
      if (game.playable)
        BadRequest("Only bots can access their games in progress. See https://lichess.org/api#tag/Chess-Bot").fuccess
      else {
        val config = GameApiV2.OneConfig(
          format = if (HTTPRequest acceptsJson ctx.req) GameApiV2.Format.JSON else GameApiV2.Format.PGN,
          imported = getBool("imported"),
          flags = requestPgnFlags(ctx.req, extended = true)
        )
        lila.mon.export.pgn.game.increment()
        env.api.gameApiV2.exportOne(game, config) flatMap { content =>
          env.api.gameApiV2.filename(game, config.format) map { filename =>
            Ok(content).withHeaders(
              CONTENT_DISPOSITION -> s"attachment; filename=$filename"
            ) as gameContentType(config)
          }
        }
      }
    }
  }

  def exportByUser(username: String) = OpenOrScoped()(
    open = ctx => handleExport(username, ctx.me, ctx.req, oauth = false),
    scoped = req => me => handleExport(username, me.some, req, oauth = true)
  )

  def apiExportByUser(username: String) = AnonOrScoped()(
    anon = req => handleExport(username, none, req, oauth = false),
    scoped = req => me => handleExport(username, me.some, req, oauth = true)
  )

  private def handleExport(username: String, me: Option[lila.user.User], req: RequestHeader, oauth: Boolean) =
    env.user.repo named username flatMap {
      _ ?? { user =>
        apiC.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
          apiC.GlobalLinearLimitPerUserOption(me) {
            val format = GameApiV2.Format byRequest req
            WithVs(req) { vs =>
              val config = GameApiV2.ByUserConfig(
                user = user,
                format = format,
                vs = vs,
                since = getLong("since", req) map { new DateTime(_) },
                until = getLong("until", req) map { new DateTime(_) },
                max = getInt("max", req) map (_ atLeast 1),
                rated = getBoolOpt("rated", req),
                perfType = ~get("perfType", req) split "," flatMap { lila.rating.PerfType(_) } toSet,
                color = get("color", req) flatMap chess.Color.apply,
                analysed = getBoolOpt("analysed", req),
                ongoing = getBool("ongoing", req),
                flags = requestPgnFlags(req, extended = false).copy(
                  literate = false
                ),
                perSecond = MaxPerSecond(me match {
                  case Some(m) if m is user.id => 50
                  case Some(_) if oauth        => 25 // bonus for oauth logged in only (not for CSRF)
                  case _                       => 15
                })
              )
              val date = DateTimeFormat forPattern "yyyy-MM-dd" print new DateTime
              Ok.chunked(env.api.gameApiV2.exportByUser(config))
                .withHeaders(
                  noProxyBufferHeader,
                  CONTENT_DISPOSITION -> s"attachment; filename=lichess_${user.username}_$date.${format.toString.toLowerCase}"
                )
                .as(gameContentType(config))
                .fuccess
            }
          }
        }
      }
    }

  def exportByIds = Action.async(parse.tolerantText) { req =>
    apiC.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
      val config = GameApiV2.ByIdsConfig(
        ids = req.body.split(',').view.take(300).toSeq,
        format = GameApiV2.Format byRequest req,
        flags = requestPgnFlags(req, extended = false),
        perSecond = MaxPerSecond(20)
      )
      Ok.chunked(env.api.gameApiV2.exportByIds(config))
        .withHeaders(
          noProxyBufferHeader
        )
        .as(gameContentType(config))
        .fuccess
    }
  }

  private def WithVs(req: RequestHeader)(f: Option[lila.user.User] => Fu[Result]): Fu[Result] =
    get("vs", req) match {
      case None => f(none)
      case Some(name) =>
        env.user.repo named name flatMap {
          case None       => notFoundJson(s"No such opponent: $name")
          case Some(user) => f(user.some)
        }
    }

  private[controllers] def requestPgnFlags(req: RequestHeader, extended: Boolean) =
    lila.game.PgnDump.WithFlags(
      moves = getBoolOpt("moves", req) | true,
      tags = getBoolOpt("tags", req) | true,
      clocks = getBoolOpt("clocks", req) | extended,
      evals = getBoolOpt("evals", req) | extended,
      opening = getBoolOpt("opening", req) | extended,
      literate = getBoolOpt("literate", req) | false
    )

  private[controllers] def gameContentType(config: GameApiV2.Config) = config.format match {
    case GameApiV2.Format.PGN => pgnContentType
    case GameApiV2.Format.JSON =>
      config match {
        case _: GameApiV2.OneConfig => JSON
        case _                      => ndJsonContentType
      }
  }

  private[controllers] def preloadUsers(game: GameModel): Funit =
    env.user.lightUserApi preloadMany game.userIds
}
