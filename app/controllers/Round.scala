package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.chat.Chat
import lila.common.HTTPRequest
import lila.game.{ Pov, Game => GameModel, PgnDump, PlayerRef }
import lila.tournament.{ TourMiniView, Tournament => Tour }
import lila.user.{ User => UserModel }
import views._

final class Round(
    env: Env,
    gameC: => Game,
    challengeC: => Challenge,
    analyseC: => Analyse,
    tournamentC: => Tournament
) extends LilaController(env) with TheftPrevention {

  private def analyser = env.analyse.analyser

  private def renderPlayer(pov: Pov)(implicit ctx: Context): Fu[Result] = negotiate(
    html = if (!pov.game.started) notFound
    else PreventTheft(pov) {
      pov.game.playableByAi ?? env.fishnet.player(pov.game)
      myTour(pov.game.tournamentId, true) flatMap { tour =>
        gameC.preloadUsers(pov.game) zip
          (pov.game.simulId ?? env.simul.repo.find) zip
          getPlayerChat(pov.game, tour.map(_.tour)) zip
          (ctx.noBlind ?? env.game.crosstableApi.withMatchup(pov.game)) zip // probably what raises page mean time?
          (pov.game.isSwitchable ?? otherPovs(pov.game)) zip
          env.bookmark.api.exists(pov.game, ctx.me) zip
          env.api.roundApi.player(pov, lila.api.Mobile.Api.currentVersion) map {
            case _ ~ simul ~ chatOption ~ crosstable ~ playing ~ bookmarked ~ data =>
              simul foreach env.simul.api.onPlayerConnection(pov.game, ctx.me)
              Ok(html.round.player(pov, data,
                tour = tour,
                simul = simul,
                cross = crosstable,
                playing = playing,
                chatOption = chatOption,
                bookmarked = bookmarked))
          }
      }
    }.mon(_.http.response.player.website),
    api = apiVersion => {
      if (isTheft(pov)) fuccess(theftResponse)
      else {
        pov.game.playableByAi ?? env.fishnet.player(pov.game)
        gameC.preloadUsers(pov.game) zip
          env.api.roundApi.player(pov, apiVersion) zip
          getPlayerChat(pov.game, none) map {
            case _ ~ data ~ chat => Ok {
              data.add("chat", chat.flatMap(_.game).map(c => lila.chat.JsonView(c.chat)))
            }
          }
      }
    }.mon(_.http.response.player.mobile)
  ) map NoCache

  def player(fullId: String) = Open { implicit ctx =>
    OptionFuResult(env.round.proxyRepo.pov(fullId)) { pov =>
      env.round.checkOutoftime(pov.game)
      renderPlayer(pov)
    }
  }

  private def otherPovs(game: GameModel)(implicit ctx: Context) = ctx.me ?? { user =>
    env.round.proxyRepo urgentGames user map {
      _ filter { pov =>
        pov.gameId != game.id && pov.game.isSwitchable && pov.game.isSimul == game.isSimul
      }
    }
  }

  private def getNext(currentGame: GameModel)(povs: List[Pov])(implicit ctx: Context) =
    povs find { pov =>
      pov.isMyTurn && (pov.game.hasClock || !currentGame.hasClock)
    }

  def whatsNext(fullId: String) = Open { implicit ctx =>
    OptionFuResult(env.round.proxyRepo.pov(fullId)) { currentPov =>
      if (currentPov.isMyTurn) fuccess {
        Ok(Json.obj("nope" -> true))
      }
      else otherPovs(currentPov.game) map getNext(currentPov.game) map { next =>
        Ok(Json.obj("next" -> next.map(_.fullId)))
      }
    }
  }

  def next(gameId: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.round.proxyRepo game gameId) { currentGame =>
      otherPovs(currentGame) map getNext(currentGame) map {
        _ orElse Pov(currentGame, me)
      } flatMap {
        case Some(next) => renderPlayer(next)
        case None => fuccess(Redirect(currentGame.simulId match {
          case Some(simulId) => routes.Simul.show(simulId)
          case None => routes.Round.watcher(gameId, "white")
        }))
      }
    }
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx =>
    proxyPov(gameId, color) flatMap {
      case Some(pov) => get("pov") match {
        case Some(requestedPov) => (pov.player.userId, pov.opponent.userId) match {
          case (Some(_), Some(opponent)) if opponent == requestedPov =>
            Redirect(routes.Round.watcher(gameId, (!pov.color).name)).fuccess
          case (Some(player), Some(_)) if player == requestedPov =>
            Redirect(routes.Round.watcher(gameId, pov.color.name)).fuccess
          case _ =>
            Redirect(routes.Round.watcher(gameId, "white")).fuccess
        }
        case None => {
          env.round.checkOutoftime(pov.game)
          watch(pov)
        }
      }
      case None => challengeC showId gameId
    }
  }

  private def proxyPov(gameId: String, color: String): Fu[Option[Pov]] = chess.Color(color) ?? {
    env.round.proxyRepo.pov(gameId, _)
  }

  private[controllers] def watch(pov: Pov, userTv: Option[UserModel] = None)(implicit ctx: Context): Fu[Result] =
    playablePovForReq(pov.game) match {
      case Some(player) if userTv.isEmpty => renderPlayer(pov withColor player.color)
      case _ if pov.game.variant == chess.variant.RacingKings && pov.color.black =>
        Redirect(routes.Round.watcher(pov.gameId, "white")).fuccess
      case _ => negotiate(
        html = {
          if (pov.game.replayable) analyseC.replay(pov, userTv = userTv)
          else if (HTTPRequest.isHuman(ctx.req))
            myTour(pov.game.tournamentId, false) zip
              (pov.game.simulId ?? env.simul.repo.find) zip
              getWatcherChat(pov.game) zip
              (ctx.noBlind ?? env.game.crosstableApi.withMatchup(pov.game)) zip
              env.api.roundApi.watcher(
                pov,
                lila.api.Mobile.Api.currentVersion,
                tv = userTv.map { u => lila.round.OnUserTv(u.id) }
              ) zip
                env.bookmark.api.exists(pov.game, ctx.me) map {
                  case tour ~ simul ~ chat ~ crosstable ~ data ~ bookmarked =>
                    Ok(html.round.watcher(pov, data, tour, simul, crosstable, userTv = userTv, chatOption = chat, bookmarked = bookmarked))
                }
          else for { // web crawlers don't need the full thing
            initialFen <- env.game.gameRepo.initialFen(pov.gameId)
            pgn <- env.api.pgnDump(pov.game, initialFen, none, PgnDump.WithFlags(clocks = false))
          } yield Ok(html.round.watcher.crawler(pov, initialFen, pgn))
        }.mon(_.http.response.watcher.website),
        api = apiVersion => for {
          data <- env.api.roundApi.watcher(pov, apiVersion, tv = none)
          analysis <- analyser get pov.game
          chat <- getWatcherChat(pov.game)
        } yield Ok {
          data
            .add("chat" -> chat.map(c => lila.chat.JsonView(c.chat)))
            .add("analysis" -> analysis.map(a => lila.analyse.JsonView.mobile(pov.game, a)))
        }
      ) map { NoCache(_) }
    }

  private def myTour(tourId: Option[String], withTop: Boolean): Fu[Option[TourMiniView]] =
    tourId ?? { env.tournament.api.miniView(_, withTop) }

  private[controllers] def getWatcherChat(game: GameModel)(implicit ctx: Context): Fu[Option[lila.chat.UserChat.Mine]] = {
    ctx.noKid && ctx.me.fold(true)(env.chat.panic.allowed) && {
      game.finishedOrAborted || !ctx.userId.exists(game.userIds.contains)
    }
  } ?? {
    val id = Chat.Id(s"${game.id}/w")
    env.chat.api.userChat.findMineIf(id, ctx.me, !game.justCreated) flatMap { chat =>
      env.user.lightUserApi.preloadMany(chat.chat.userIds) inject chat.some
    }
  }

  private[controllers] def getPlayerChat(game: GameModel, tour: Option[Tour])(implicit ctx: Context): Fu[Option[Chat.GameOrEvent]] = ctx.noKid ?? {
    def toEventChat(resource: String)(c: lila.chat.UserChat.Mine) = Chat.GameOrEvent(Right((
      c truncate 100,
      lila.chat.Chat.ResourceId(resource)
    ))).some
    (game.tournamentId, game.simulId) match {
      case (Some(tid), _) => {
        ctx.isAuth && tour.fold(true)(tournamentC.canHaveChat(_, none))
      } ?? env.chat.api.userChat.cached.findMine(Chat.Id(tid), ctx.me).map(toEventChat(s"tournament/$tid"))
      case (_, Some(sid)) => game.simulId.?? { sid =>
        env.chat.api.userChat.cached.findMine(Chat.Id(sid), ctx.me).map(toEventChat(s"simul/$sid"))
      }
      case _ => game.hasChat ?? {
        env.chat.api.playerChat.findIf(Chat.Id(game.id), !game.justCreated) map { chat =>
          Chat.GameOrEvent(Left(Chat.Restricted(
            chat,
            restricted = game.fromLobby && ctx.isAnon
          ))).some
        }
      }
    }
  }

  def sides(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(proxyPov(gameId, color)) { pov =>
      (pov.game.tournamentId ?? env.tournament.tournamentRepo.byId) zip
        (pov.game.simulId ?? env.simul.repo.find) zip
        env.game.gameRepo.initialFen(pov.game) zip
        env.game.crosstableApi.withMatchup(pov.game) zip
        env.bookmark.api.exists(pov.game, ctx.me) map {
          case tour ~ simul ~ initialFen ~ crosstable ~ bookmarked =>
            Ok(html.game.bits.sides(pov, initialFen, tour, crosstable, simul, bookmarked = bookmarked))
        }
    }
  }

  def writeNote(gameId: String) = AuthBody { implicit ctx => me =>
    import play.api.data.Forms._
    import play.api.data._
    implicit val req = ctx.body
    Form(single("text" -> text)).bindFromRequest.fold(
      err => fuccess(BadRequest),
      text => env.round.noteApi.set(gameId, me.id, text.trim take 10000)
    )
  }

  def readNote(gameId: String) = Auth { implicit ctx => me =>
    env.round.noteApi.get(gameId, me.id) map { text =>
      Ok(text)
    }
  }

  def continue(id: String, mode: String) = Open { implicit ctx =>
    OptionResult(env.game.gameRepo game id) { game =>
      Redirect("%s?fen=%s#%s".format(
        routes.Lobby.home(),
        get("fen") | (chess.format.Forsyth >> game.chess),
        mode
      ))
    }
  }

  def resign(fullId: String) = Open { implicit ctx =>
    OptionFuRedirect(env.round.proxyRepo.pov(fullId)) { pov =>
      if (isTheft(pov)) {
        lila.log("round").warn(s"theft resign $fullId ${HTTPRequest.lastRemoteAddress(ctx.req)}")
        fuccess(routes.Lobby.home)
      } else {
        env.round resign pov
        import scala.concurrent.duration._
        akka.pattern.after(500 millis, env.system.scheduler)(fuccess(routes.Lobby.home))
      }
    }
  }

  def mini(gameId: String, color: String) = Open { implicit ctx =>
    OptionOk(chess.Color(color).??(env.round.proxyRepo.povIfPresent(gameId, _)) orElse env.game.gameRepo.pov(gameId, color))(html.game.bits.mini)
  }

  def miniFullId(fullId: String) = Open { implicit ctx =>
    OptionOk(env.round.proxyRepo.povIfPresent(fullId) orElse env.game.gameRepo.pov(fullId))(html.game.bits.mini)
  }
}
