package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.hub.LightTeam
import lila.security.Granter
import lila.team.{ Joined, Motivate, Team => TeamModel }
import lila.user.{ User => UserModel }
import views._

final class Team(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def forms     = env.team.forms
  private def api       = env.team.api
  private def paginator = env.team.paginator

  def all(page: Int) = Open { implicit ctx =>
    paginator popularTeams page map {
      html.team.list.all(_)
    }
  }

  def home(page: Int) = Open { implicit ctx =>
    ctx.me.??(api.hasTeams) map {
      case true  => Redirect(routes.Team.mine)
      case false => Redirect(routes.Team.all(page))
    }
  }

  def show(id: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(api team id) { renderTeam(_, page) }
  }

  def search(text: String, page: Int) = OpenBody { implicit ctx =>
    if (text.trim.isEmpty) paginator popularTeams page map { html.team.list.all(_) } else
      env.teamSearch(text, page) map { html.team.list.search(text, _) }
  }

  private def renderTeam(team: TeamModel, page: Int = 1)(implicit ctx: Context) =
    for {
      info    <- env.teamInfo(team, ctx.me)
      members <- paginator.teamMembers(team, page)
      hasChat = canHaveChat(team, info)
      chat <- hasChat ?? env.chat.api.userChat.cached
        .findMine(lila.chat.Chat.Id(team.id), ctx.me)
        .map(some)
      _ <- env.user.lightUserApi preloadMany {
        info.userIds ::: chat.??(_.chat.userIds)
      }
      version <- hasChat ?? env.team.version(team.id).dmap(some)
    } yield html.team.show(team, members, info, chat, version)

  private def canHaveChat(team: TeamModel, info: lila.app.mashup.TeamInfo)(implicit ctx: Context): Boolean =
    !team.isChatFor(_.NONE) && ctx.noKid && {
      (team.isChatFor(_.LEADERS) && ctx.userId.exists(team.leaders)) ||
      (team.isChatFor(_.MEMBERS) && info.mine) ||
      isGranted(_.ChatTimeout)
    }

  def legacyUsers(teamId: String) = Action {
    MovedPermanently(routes.Team.users(teamId).url)
  }

  def users(teamId: String) = Action.async { implicit req =>
    api.team(teamId) flatMap {
      _ ?? { team =>
        apiC.jsonStream {
          env.team
            .memberStream(team, MaxPerSecond(20))
            .map(env.api.userApi.one)
        }.fuccess
      }
    }
  }

  def tournaments(teamId: String) = Open { implicit ctx =>
    env.team.teamRepo.enabled(teamId) flatMap {
      _ ?? { team =>
        env.tournament.tournamentRepo.visibleByTeam(team.id, team.leaders, 50) map { tours =>
          Ok(html.team.bits.tournaments(team, tours))
        }
      }
    }
  }

  def edit(id: String) = Auth { implicit ctx => _ =>
    WithOwnedTeam(id) { team =>
      fuccess(html.team.form.edit(team, forms edit team))
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    WithOwnedTeam(id) { team =>
      implicit val req = ctx.body
      forms
        .edit(team)
        .bindFromRequest
        .fold(
          err => BadRequest(html.team.form.edit(team, err)).fuccess,
          data => api.update(team, data, me) inject Redirect(routes.Team.show(team.id)).flashSuccess
        )
    }
  }

  def kickForm(id: String) = Auth { implicit ctx => me =>
    WithOwnedTeam(id) { team =>
      env.team.memberRepo userIdsByTeam team.id map { userIds =>
        html.team.admin.kick(team, userIds - me.id)
      }
    }
  }

  def kick(id: String) = AuthBody { implicit ctx => me =>
    WithOwnedTeam(id) { team =>
      implicit val req = ctx.body
      forms.selectMember.bindFromRequest.value ?? { api.kick(team, _, me) } inject Redirect(
        routes.Team.show(team.id)
      ).flashSuccess
    }
  }
  def kickUser(teamId: String, userId: String) = Scoped(_.Team.Write) { _ => me =>
    api team teamId flatMap {
      _ ?? { team =>
        if (team leaders me.id) api.kick(team, userId, me) inject jsonOkResult
        else Forbidden(jsonError("Not your team")).fuccess
      }
    }
  }

  def leadersForm(id: String) = Auth { implicit ctx => _ =>
    WithOwnedTeam(id) { team =>
      Ok(html.team.admin.leaders(team, forms leaders team)).fuccess
    }
  }

  def leaders(id: String) = AuthBody { implicit ctx => _ =>
    WithOwnedTeam(id) { team =>
      implicit val req = ctx.body
      forms.leaders(team).bindFromRequest.value ?? { api.setLeaders(team, _) } inject Redirect(
        routes.Team.show(team.id)
      ).flashSuccess
    }
  }

  def close(id: String) = Secure(_.ManageTeam) { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      (api delete team) >>
        env.mod.logApi.deleteTeam(me.id, team.name, team.description) inject
        Redirect(routes.Team all 1).flashSuccess
    }
  }

  def form = Auth { implicit ctx => me =>
    OnePerWeek(me) {
      forms.anyCaptcha map { captcha =>
        Ok(html.team.form.create(forms.create, captcha))
      }
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    OnePerWeek(me) {
      implicit val req = ctx.body
      forms.create.bindFromRequest.fold(
        err =>
          forms.anyCaptcha map { captcha =>
            BadRequest(html.team.form.create(err, captcha))
          },
        data =>
          api.create(data, me) map { team =>
            Redirect(routes.Team.show(team.id)).flashSuccess
          }
      )
    }
  }

  def mine = Auth { implicit ctx => me =>
    api mine me map {
      html.team.list.mine(_)
    }
  }

  def leader = Auth { implicit ctx => me =>
    env.team.teamRepo enabledTeamsByLeader me.id map {
      html.team.list.ledByMe(_)
    }
  }

  def join(id: String) = AuthOrScoped(_.Team.Write)(
    auth = implicit ctx =>
      me =>
        api countTeamsOf me flatMap { nb =>
          if (nb >= TeamModel.maxJoin)
            negotiate(
              html = BadRequest(views.html.site.message.teamJoinLimit).fuccess,
              api = _ => BadRequest(jsonError("You have joined too many teams")).fuccess
            )
          else
            negotiate(
              html = api.join(id, me) flatMap {
                case Some(Joined(team))   => Redirect(routes.Team.show(team.id)).flashSuccess.fuccess
                case Some(Motivate(team)) => Redirect(routes.Team.requestForm(team.id)).flashSuccess.fuccess
                case _                    => notFound(ctx)
              },
              api = _ =>
                api.join(id, me) flatMap {
                  case Some(Joined(_)) => jsonOkResult.fuccess
                  case Some(Motivate(_)) =>
                    BadRequest(
                      jsonError("This team requires confirmation.")
                    ).fuccess
                  case _ => notFoundJson("Team not found")
                }
            )
        },
    scoped = req =>
      me =>
        env.oAuth.server.fetchAppAuthor(req) flatMap {
          _ ?? { api.joinApi(id, me, _) }
        } flatMap {
          case Some(Joined(_)) => jsonOkResult.fuccess
          case Some(Motivate(_)) =>
            Forbidden(
              jsonError("This team requires confirmation, and is not owned by the oAuth app owner.")
            ).fuccess
          case _ => notFoundJson("Team not found")
        }
  )

  def requests = Auth { implicit ctx => me =>
    import lila.memo.CacheApi._
    env.team.cached.nbRequests invalidate me.id
    api requestsWithUsers me map { html.team.request.all(_) }
  }

  def requestForm(id: String) = Auth { implicit ctx => me =>
    OptionFuOk(api.requestable(id, me)) { team =>
      forms.anyCaptcha map { html.team.request.requestForm(team, forms.request, _) }
    }
  }

  def requestCreate(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.requestable(id, me)) { team =>
      implicit val req = ctx.body
      forms.request.bindFromRequest.fold(
        err =>
          forms.anyCaptcha map { captcha =>
            BadRequest(html.team.request.requestForm(team, err, captcha))
          },
        setup => api.createRequest(team, setup, me) inject Redirect(routes.Team.show(team.id)).flashSuccess
      )
    }
  }

  def requestProcess(requestId: String) = AuthBody { implicit ctx => me =>
    OptionFuRedirectUrl(for {
      requestOption <- api request requestId
      teamOption    <- requestOption.??(req => env.team.teamRepo.byLeader(req.team, me.id))
    } yield (teamOption |@| requestOption).tupled) {
      case (team, request) =>
        implicit val req = ctx.body
        forms.processRequest.bindFromRequest.fold(
          _ => fuccess(routes.Team.show(team.id).toString), {
            case (decision, url) =>
              api.processRequest(team, request, (decision == "accept")) inject url
          }
        )
    }
  }

  def quit(id: String) = AuthOrScoped(_.Team.Write)(
    auth = ctx =>
      me =>
        OptionResult(api.quit(id, me)) { team =>
          Redirect(routes.Team.show(team.id)).flashSuccess
        }(ctx),
    scoped = _ =>
      me =>
        api.quit(id, me) flatMap {
          _.fold(notFoundJson())(_ => jsonOkResult.fuccess)
        }
  )

  def autocomplete = Action.async { req =>
    get("term", req).filter(_.nonEmpty) match {
      case None => BadRequest("No search term provided").fuccess
      case Some(term) =>
        for {
          teams <- api.autocomplete(term, 10)
          _     <- env.user.lightUserApi preloadMany teams.map(_.createdBy)
        } yield Ok {
          JsArray(teams map { team =>
            Json.obj(
              "id"      -> team.id,
              "name"    -> team.name,
              "owner"   -> env.user.lightUserApi.sync(team.createdBy).fold(team.createdBy)(_.name),
              "members" -> team.nbMembers
            )
          })
        } as JSON
    }
  }

  def pmAll(id: String) = Auth { implicit ctx => _ =>
    WithOwnedTeam(id) { team =>
      env.tournament.api
        .featuredInTeam(team.id)
        .dmap(_.filter(_.isEnterable))
        .map { tours =>
          Ok(html.team.admin.pmAll(team, forms.pmAll, tours))
        }
    }
  }

  def pmAllSubmit(id: String) = AuthOrScopedBody(_.Team.Write)(
    auth = implicit ctx =>
      me =>
        WithOwnedTeam(id) { team =>
          doPmAll(team, me)(ctx.body).fold(
            err =>
              env.tournament.api
                .featuredInTeam(team.id)
                .dmap(_.filter(_.isEnterable))
                .map { tours =>
                  BadRequest(html.team.admin.pmAll(team, err, tours))
                },
            done => done inject Redirect(routes.Team.show(team.id)).flashSuccess
          )
        },
    scoped = implicit req =>
      me =>
        api team id flatMap {
          _.filter(_ leaders me.id) ?? { team =>
            doPmAll(team, me).fold(
              err => BadRequest(errorsAsJson(err)(reqLang)).fuccess,
              done => done inject jsonOkResult
            )
          }
        }
  )

  // API

  def apiAll(page: Int) = Action.async {
    import env.team.jsonView._
    import lila.common.paginator.PaginatorJson._
    JsonFuOk {
      paginator popularTeams page flatMap { pager =>
        env.user.lightUserApi.preloadMany(pager.currentPageResults.flatMap(_.leaders)) inject pager
      }
    }
  }

  def apiShow(id: String) = Action.async {
    import env.team.jsonView._
    JsonOptionOk { api team id }
  }

  def apiSearch(text: String, page: Int) = Action.async {
    import env.team.jsonView._
    import lila.common.paginator.PaginatorJson._
    JsonFuOk {
      if (text.trim.isEmpty) paginator popularTeams page
      else env.teamSearch(text, page)
    }
  }

  def apiTeamsOf(username: String) = Action.async {
    import env.team.jsonView._
    JsonFuOk {
      api teamsOf username flatMap { teams =>
        env.user.lightUserApi.preloadMany(teams.flatMap(_.leaders)) inject teams
      }
    }
  }

  private def doPmAll(team: TeamModel, me: UserModel)(implicit req: Request[_]): Either[Form[_], Funit] =
    forms.pmAll.bindFromRequest
      .fold(
        err => Left(err),
        msg =>
          Right {
            PmAllLimitPerUser(me.id) {
              val full = s"""$msg
---
You received this message because you are part of the team lichess.org${routes.Team.show(team.id)}."""
              env.msg.api.multiPost(me, env.team.memberStream.ids(team, MaxPerSecond(50)), full)
              funit // we don't wait for the stream to complete, it would make lichess time out
            }
          }
      )

  private val PmAllLimitPerUser = lila.memo.RateLimit.composite[lila.user.User.ID](
    name = "team pm all per user",
    key = "team.pmAll",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 1, 3.minutes),
    ("slow", 6, 24.hours)
  )

  private def OnePerWeek[A <: Result](me: UserModel)(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    api.countCreatedRecently(me) flatMap { count =>
      if (count > 10 || (count > 3 && !Granter(_.Teacher)(me) && !Granter(_.ManageTeam)(me)))
        Forbidden(views.html.site.message.teamCreateLimit).fuccess
      else a
    }

  private def WithOwnedTeam(teamId: String)(f: TeamModel => Fu[Result])(implicit ctx: Context): Fu[Result] =
    OptionFuResult(api team teamId) { team =>
      if (ctx.userId.exists(team.leaders.contains) || isGranted(_.ManageTeam)) f(team)
      else renderTeam(team) map { Forbidden(_) }
    }

  private[controllers] def teamsIBelongTo(me: lila.user.User): Fu[List[LightTeam]] =
    api mine me map { _.map(_.light) }
}
