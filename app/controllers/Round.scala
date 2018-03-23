package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.chat.Chat
import lila.common.HTTPRequest
import lila.game.{ Pov, GameRepo, Game => GameModel, PgnDump, PlayerRef }
import lila.tournament.{ TourMiniView, Tournament => Tour }
import lila.user.{ User => UserModel }
import views._

object Round extends LilaController with TheftPrevention {

  private def env = Env.round
  private def analyser = Env.analyse.analyser
  private val logger = lila.log("round")

  def websocketWatcher(gameId: String, color: String) = SocketOption[JsValue] { implicit ctx =>
    proxyPov(gameId, color) flatMap {
      _ ?? { pov =>
        getSocketUid("sri") ?? { uid =>
          env.socketHandler.watcher(
            pov = pov,
            uid = uid,
            user = ctx.me,
            ip = ctx.ip,
            userTv = get("userTv")
          ) map some
        }
      }
    }
  }

  def websocketPlayer(fullId: String, apiVersion: Int) = SocketEither[JsValue] { implicit ctx =>
    proxyPov(fullId) flatMap {
      case Some(pov) =>
        if (isTheft(pov)) fuccess(Left(theftResponse))
        else getSocketUid("sri") match {
          case Some(uid) =>
            requestAiMove(pov) >>
              env.socketHandler.player(pov, uid, ctx.me, ctx.ip) map Right.apply
          case None => fuccess(Left(NotFound))
        }
      case None => fuccess(Left(NotFound))
    }
  }

  private def requestAiMove(pov: Pov) = pov.game.playableByAi ?? Env.fishnet.player(pov.game)

  private def renderPlayer(pov: Pov)(implicit ctx: Context): Fu[Result] = negotiate(
    html = pov.game.started.fold(
      PreventTheft(pov) {
        myTour(pov.game.tournamentId, true) flatMap { tour =>
          Game.preloadUsers(pov.game) zip
            (pov.game.simulId ?? Env.simul.repo.find) zip
            getPlayerChat(pov.game, tour.map(_.tour)) zip
            Env.game.crosstableApi.withMatchup(pov.game) zip // probably what raises page mean time?
            (pov.game.isSwitchable ?? otherPovs(pov.game)) zip
            Env.bookmark.api.exists(pov.game, ctx.me) zip
            Env.api.roundApi.player(pov, lila.api.Mobile.Api.currentVersion) map {
              case _ ~ simul ~ chatOption ~ crosstable ~ playing ~ bookmarked ~ data =>
                simul foreach Env.simul.api.onPlayerConnection(pov.game, ctx.me)
                Ok(html.round.player(pov, data,
                  tour = tour,
                  simul = simul.filter(_ isHost ctx.me),
                  cross = crosstable,
                  playing = playing,
                  chatOption = chatOption,
                  bookmarked = bookmarked))
            }
        }
      }.mon(_.http.response.player.website),
      notFound
    ),
    api = apiVersion => {
      if (isTheft(pov)) fuccess(theftResponse)
      else Game.preloadUsers(pov.game) zip
        Env.api.roundApi.player(pov, apiVersion) zip
        getPlayerChat(pov.game, none) map {
          case _ ~ data ~ chat => Ok {
            data.add("chat", chat.flatMap(_.game).map(c => lila.chat.JsonView(c.chat)))
          }
        }
    }.mon(_.http.response.player.mobile)
  ) map NoCache

  def player(fullId: String) = Open { implicit ctx =>
    proxyBench(1000)
    OptionFuResult(proxyPov(fullId)) { pov =>
      env.checkOutoftime(pov.game)
      renderPlayer(pov)
    }
  }

  private def proxyBench(nb: Int)(implicit ctx: Context): Funit =
    ctx.me.?? { me =>
      import lila.db.dsl._
      GameRepo.coll.find($empty, $id(true)).skip(scala.util.Random nextInt 9000).list[Bdoc](nb).flatMap {
        _.map { doc =>
          doc.getAs[String]("_id") ?? { id =>
            proxyPov(id, "white")
          }
        }.sequenceFu
      }
    }.map { povs =>
      println(s"proxyBench ${povs.size}")
    }

  private def otherPovs(game: GameModel)(implicit ctx: Context) = ctx.me ?? { user =>
    GameRepo urgentGames user map {
      _ filter { pov =>
        pov.game.id != game.id && pov.game.isSwitchable && pov.game.isSimul == game.isSimul
      }
    }
  }

  private def getNext(currentGame: GameModel)(povs: List[Pov])(implicit ctx: Context) =
    povs find { pov =>
      pov.isMyTurn && (pov.game.hasClock || !currentGame.hasClock)
    }

  def whatsNext(fullId: String) = Open { implicit ctx =>
    OptionFuResult(proxyPov(fullId)) { currentPov =>
      if (currentPov.isMyTurn) fuccess {
        Ok(Json.obj("nope" -> true))
      }
      else otherPovs(currentPov.game) map getNext(currentPov.game) map { next =>
        Ok(Json.obj("next" -> next.map(_.fullId)))
      }
    }
  }

  def next(gameId: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game gameId) { currentGame =>
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
    env.actualRoundProxyGame(gameId).effectFold(
      _ => lila.mon.round.proxyGameWatcher("exception")(),
      g => lila.mon.round.proxyGameWatcher(g.isDefined.toString)()
    )
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
          env.checkOutoftime(pov.game)
          watch(pov)
        }
      }
      case None => Challenge showId gameId
    }
  }

  private def proxyPov(gameId: String, color: String): Fu[Option[Pov]] = chess.Color(color) ?? { c =>
    env.roundProxyGame(gameId) map2 { (g: GameModel) => g pov c }
  }
  private def proxyPov(fullId: String): Fu[Option[Pov]] = {
    val ref = PlayerRef(fullId)
    env.roundProxyGame(ref.gameId) map {
      _ flatMap { _ playerIdPov ref.playerId }
    }
  }

  private[controllers] def watch(pov: Pov, userTv: Option[UserModel] = None)(implicit ctx: Context): Fu[Result] =
    playablePovForReq(pov.game) match {
      case Some(player) if userTv.isEmpty => renderPlayer(pov withColor player.color)
      case _ => Game.preloadUsers(pov.game) >> negotiate(
        html = env.getSocketStatus(pov.game.id).flatMap { socketStatusPre =>
          if (getBool("sudo") && isGranted(_.SuperAdmin)) Redirect(routes.Round.player(pov.fullId)).fuccess
          else if (pov.game.replayable) Analyse.replay(pov, userTv = userTv)
          else if (HTTPRequest.isHuman(ctx.req))
            myTour(pov.game.tournamentId, false) zip
              (pov.game.simulId ?? Env.simul.repo.find) zip
              getWatcherChat(pov.game) zip
              Env.game.crosstableApi.withMatchup(pov.game) zip
              Env.api.roundApi.watcher(
                pov,
                lila.api.Mobile.Api.currentVersion,
                tv = userTv.map { u => lila.round.OnUserTv(u.id) }
              ) zip
                Env.bookmark.api.exists(pov.game, ctx.me) map {
                  case tour ~ simul ~ chat ~ crosstable ~ data ~ bookmarked =>
                    (data \ "player" \ "version").asOpt[Int].fold(logger.warn(s"watch JSON no socket version $pov")) { v =>
                      if (v != socketStatusPre.version) logger.warn(s"watch JSON socket version doesn't match $pov $v $socketStatusPre")
                    }
                    Ok(html.round.watcher(pov, data, tour, simul, crosstable, userTv = userTv, chatOption = chat, bookmarked = bookmarked))
                }
          else for { // web crawlers don't need the full thing
            initialFen <- GameRepo.initialFen(pov.game.id)
            pgn <- Env.api.pgnDump(pov.game, initialFen, PgnDump.WithFlags(clocks = false))
          } yield Ok(html.round.watcherBot(pov, initialFen, pgn))
        }.mon(_.http.response.watcher.website),
        api = apiVersion => for {
          data <- Env.api.roundApi.watcher(pov, apiVersion, tv = none)
          analysis <- pov.game.metadata.analysed.??(analyser get pov.game.id)
          chat <- getWatcherChat(pov.game)
        } yield Ok {
          data
            .add("c" -> chat.map(c => lila.chat.JsonView(c.chat)))
            .add("analysis" -> analysis.map(a => lila.analyse.JsonView.mobile(pov.game, a)))
        }
      ) map NoCache
    }

  private def myTour(tourId: Option[String], withTop: Boolean): Fu[Option[TourMiniView]] =
    tourId ?? { Env.tournament.api.miniView(_, withTop) }

  private[controllers] def getWatcherChat(game: GameModel)(implicit ctx: Context): Fu[Option[lila.chat.UserChat.Mine]] = {
    ctx.noKid && ctx.me.exists(Env.chat.panic.allowed)
  } ?? {
    Env.chat.api.userChat.findMineIf(Chat.Id(s"${game.id}/w"), ctx.me, !game.justCreated) flatMap { chat =>
      Env.user.lightUserApi.preloadMany(chat.chat.userIds) inject chat.some
    }
  }

  private[controllers] def getPlayerChat(game: GameModel, tour: Option[Tour])(implicit ctx: Context): Fu[Option[Chat.GameOrEvent]] = ctx.noKid ?? {
    (game.tournamentId, game.simulId) match {
      case (Some(tid), _) => {
        ctx.isAuth && tour.fold(true)(Tournament.canHaveChat)
      } ??
        Env.chat.api.userChat.cached.findMine(Chat.Id(tid), ctx.me).map { chat =>
          Chat.GameOrEvent(Right(chat truncate 50)).some
        }
      case (_, Some(sid)) => game.simulId.?? { sid =>
        Env.chat.api.userChat.cached.findMine(Chat.Id(sid), ctx.me).map { chat =>
          Chat.GameOrEvent(Right(chat truncate 50)).some
        }
      }
      case _ => game.hasChat ?? {
        Env.chat.api.playerChat.findIf(Chat.Id(game.id), !game.justCreated).map { chat =>
          Chat.GameOrEvent(Left(Chat.Restricted(chat, game.fromLobby && ctx.isAnon))).some
        }
      }
    }
  }

  def playerText(fullId: String) = Open { implicit ctx =>
    OptionResult(GameRepo pov fullId) { pov =>
      if (ctx.blindMode) Ok(html.game.textualRepresentation(pov, true))
      else BadRequest
    }
  }

  def watcherText(gameId: String, color: String) = Open { implicit ctx =>
    OptionResult(GameRepo.pov(gameId, color)) { pov =>
      if (ctx.blindMode) Ok(html.game.textualRepresentation(pov, false))
      else BadRequest
    }
  }

  def sides(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo.pov(gameId, color)) { pov =>
      (pov.game.tournamentId ?? lila.tournament.TournamentRepo.byId) zip
        (pov.game.simulId ?? Env.simul.repo.find) zip
        GameRepo.initialFen(pov.game) zip
        Env.game.crosstableApi.withMatchup(pov.game) zip
        Env.bookmark.api.exists(pov.game, ctx.me) map {
          case tour ~ simul ~ initialFen ~ crosstable ~ bookmarked =>
            Ok(html.game.sides(pov, initialFen, tour, crosstable, simul, bookmarked = bookmarked))
        }
    }
  }

  def writeNote(gameId: String) = AuthBody { implicit ctx => me =>
    import play.api.data.Forms._
    import play.api.data._
    implicit val req = ctx.body
    Form(single("text" -> text)).bindFromRequest.fold(
      err => fuccess(BadRequest),
      text => Env.round.noteApi.set(gameId, me.id, text.trim take 10000)
    )
  }

  def readNote(gameId: String) = Auth { implicit ctx => me =>
    Env.round.noteApi.get(gameId, me.id) map { text =>
      Ok(text)
    }
  }

  def continue(id: String, mode: String) = Open { implicit ctx =>
    OptionResult(GameRepo game id) { game =>
      Redirect("%s?fen=%s#%s".format(
        routes.Lobby.home(),
        get("fen") | (chess.format.Forsyth >> game.chess),
        mode
      ))
    }
  }

  def resign(fullId: String) = Open { implicit ctx =>
    OptionFuRedirect(GameRepo pov fullId) { pov =>
      if (isTheft(pov)) {
        controllerLogger.warn(s"theft resign $fullId ${HTTPRequest.lastRemoteAddress(ctx.req)}")
        fuccess(routes.Lobby.home)
      } else {
        env resign pov
        import scala.concurrent.duration._
        val scheduler = lila.common.PlayApp.system.scheduler
        akka.pattern.after(500 millis, scheduler)(fuccess(routes.Lobby.home))
      }
    }
  }

  def mini(gameId: String, color: String) = Open { implicit ctx =>
    OptionOk(GameRepo.pov(gameId, color)) { pov =>
      html.game.mini(pov)
    }
  }

  def miniFullId(fullId: String) = Open { implicit ctx =>
    OptionOk(GameRepo pov fullId) { pov =>
      html.game.mini(pov)
    }
  }

  def atom(gameId: String, color: String) = Action.async { implicit req =>
    GameRepo.pov(gameId, color) flatMap {
      case Some(pov) => GameRepo initialFen pov.game map { initialFen =>
        val pgn = Env.game.pgnDump(pov.game, initialFen, PgnDump.WithFlags(clocks = false))
        Ok(views.xml.round.atom(pov, pgn)) as XML
      }
      case _ => NotFound("no such game").fuccess
    }
  }
}
