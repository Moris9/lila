package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Pov, GameRepo }
import lila.tournament.{ System, TournamentRepo, PairingRepo, Tournament => Tourney, VisibleTournaments }
import lila.user.UserRepo
import views._

object Tournament extends LilaController {

  private def env = Env.tournament
  private def repo = TournamentRepo

  private def tournamentNotFound(implicit ctx: Context) = NotFound(html.tournament.notFound())

  val home = Open { implicit ctx =>
    env.api.fetchVisibleTournaments zip
      repo.scheduledDedup zip
      repo.finished(30) zip
      UserRepo.allSortToints(10) map {
        case (((visible, scheduled), finished), leaderboard) =>
          Ok(html.tournament.home(scheduled, finished, leaderboard, env scheduleJsonView visible))
      } map NoCache
  }

  def help(sysStr: Option[String]) = Open { implicit ctx =>
    val system = sysStr flatMap {
      case "arena" => System.Arena.some
      case _       => none
    }
    Ok(html.tournament.faqPage(system)).fuccess
  }

  def show(id: String) = Open { implicit ctx =>
    val page = getInt("page")
    negotiate(
      html = repo byId id flatMap {
        _.fold(tournamentNotFound.fuccess) { tour =>
          env.version(tour.id) zip env.jsonView(tour, page, ctx.userId) zip chatOf(tour) map {
            case ((version, data), chat) => html.tournament.show(tour, version, data, chat)
          }
        }
      },
      api = _ => repo byId id flatMap {
        case None       => NotFound(Json.obj("error" -> "No such tournament")).fuccess
        case Some(tour) => env.jsonView(tour, page, ctx.userId) map { Ok(_) }
      } map (_ as JSON)
    ) map NoCache
  }

  def standing(id: String, page: Int) = Open { implicit ctx =>
    OptionFuResult(repo byId id) { tour =>
      env.jsonView.standing(tour, page) map { data =>
        Ok(data) as JSON
      }
    }
  }

  def gameStanding(id: String) = Open { implicit ctx =>
    env.api.miniStanding(id, true) map {
      case Some(m) if !m.tour.isCreated => Ok(html.tournament.gameStanding(m))
      case _                            => NotFound
    }
  }

  def userGameNbMini(id: String, user: String, nb: Int) = Open { implicit ctx =>
    withUserGameNb(id, user, nb) { pov =>
      Ok(html.tournament.miniGame(pov))
    }
  }

  def userGameNbShow(id: String, user: String, nb: Int) = Open { implicit ctx =>
    withUserGameNb(id, user, nb) { pov =>
      Redirect(routes.Round.watcher(pov.game.id, pov.color.name))
    }
  }

  private def withUserGameNb(id: String, user: String, nb: Int)(withPov: Pov => Result)(implicit ctx: Context): Fu[Result] = {
    val userId = lila.user.User normalize user
    OptionFuResult(PairingRepo.byTourUserNb(id, userId, nb)) { pairing =>
      GameRepo game pairing.id map {
        _.flatMap { Pov.ofUserId(_, userId) }.fold(NotFound("nope"))(withPov)
      }
    }
  }

  def join(id: String) = Auth { implicit ctx =>
    implicit me =>
      NoLame {
        negotiate(
          html = repo enterableById id map {
            case None => tournamentNotFound
            case Some(tour) =>
              env.api.join(tour.id, me)
              Redirect(routes.Tournament.show(tour.id))
          },
          api = _ => OptionFuOk(repo enterableById id) { tour =>
            env.api.join(tour.id, me)
            fuccess(Json.obj("ok" -> true))
          }
        )
      }
  }

  def withdraw(id: String) = Auth { implicit ctx =>
    me =>
      OptionResult(repo byId id) { tour =>
        env.api.withdraw(tour.id, me.id)
        if (HTTPRequest.isXhr(ctx.req)) Ok(Json.obj("ok" -> true)) as JSON
        else Redirect(routes.Tournament.show(tour.id))
      }
  }

  def form = Auth { implicit ctx =>
    me =>
      NoLame {
        Ok(html.tournament.form(env.forms.create, env.forms)).fuccess
      }
  }

  def create = AuthBody { implicit ctx =>
    implicit me =>
      NoLame {
        implicit val req = ctx.body
        env.forms.create.bindFromRequest.fold(
          err => BadRequest(html.tournament.form(err, env.forms)).fuccess,
          setup => env.api.createTournament(setup, me) map { tour =>
            Redirect(routes.Tournament.show(tour.id))
          })
      }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    (getInt("version") |@| get("sri")).tupled ?? {
      case (version, uid) => env.socketHandler.join(id, version, uid, ctx.me)
    }
  }

  private def chatOf(tour: lila.tournament.Tournament)(implicit ctx: Context) =
    ctx.isAuth ?? {
      Env.chat.api.userChat find tour.id map (_.forUser(ctx.me).some)
    }
}
