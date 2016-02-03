package controllers

import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{ Result, Results, Call, RequestHeader, Accepting }
import play.api.Play.current
import scala.concurrent.duration._

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.common.{ HTTPRequest, LilaCookie }
import lila.game.{ GameRepo, Pov, AnonCookie }
import lila.setup.{ HookConfig, ValidFen }
import lila.user.UserRepo
import views._

object Setup extends LilaController with TheftPrevention {

  private def env = Env.setup

  private val PostRateLimit = new lila.memo.RateLimit(5, 1 minute)

  def aiForm = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) {
      env.forms aiFilled get("fen") map { form =>
        html.setup.ai(
          form,
          Env.ai.aiPerfApi.intRatings,
          form("fen").value flatMap ValidFen(getBool("strict")))
      }
    }
    else fuccess {
      Redirect(routes.Lobby.home + "#ai")
    }
  }

  def ai = process(env.forms.ai) { config =>
    implicit ctx =>
      env.processor ai config map { pov =>
        pov -> routes.Round.player(pov.fullId)
      }
  }

  def friendForm(userId: Option[String]) = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) {
      env.forms friendFilled get("fen") flatMap { form =>
        val validFen = form("fen").value flatMap ValidFen(false)
        userId ?? UserRepo.named flatMap {
          case None => fuccess(html.setup.friend(form, none, none, validFen))
          case Some(user) => challenge(user) map { error =>
            html.setup.friend(form, user.some, error, validFen)
          }
        }
      }
    }
    else fuccess {
      Redirect(routes.Lobby.home + "#friend")
    }
  }

  private def challenge(user: lila.user.User)(implicit ctx: Context): Fu[Option[String]] = ctx.me match {
    case None => fuccess("Only registered players can send challenges.".some)
    case Some(me) => Env.relation.api.fetchBlocks(user.id, me.id) flatMap {
      case true => fuccess(s"{{user}} doesn't accept challenges from you.".some)
      case false => Env.pref.api getPref user zip Env.relation.api.fetchFollows(user.id, me.id) map {
        case (pref, follow) => lila.pref.Pref.Challenge.block(me, user, pref.challenge, follow,
          fromCheat = me.engine && !user.engine)
      }
    }
  }

  def friend(userId: Option[String]) =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      env.forms.friend(ctx).bindFromRequest.fold(
        f => negotiate(
          html = Lobby.renderHome(Results.BadRequest),
          api = _ => fuccess(BadRequest(errorsAsJson(f)))
        ), {
          case config => userId ?? UserRepo.byId flatMap { destUser =>
            import lila.challenge.Challenge._
            val challenge = lila.challenge.Challenge.make(
              variant = config.variant,
              initialFen = config.fen,
              timeControl = config.makeClock map { c =>
                TimeControl.Clock(c.limit, c.increment)
              } orElse config.makeDaysPerTurn.map {
                TimeControl.Correspondence.apply
              } getOrElse TimeControl.Unlimited,
              mode = config.mode,
              color = config.color.name,
              challenger = ctx.me,
              destUser = destUser)
            (Env.challenge.api create challenge) >> negotiate(
              html = fuccess(Redirect(routes.Round.watcher(challenge.id, "white"))),
              api = _ => Challenge showChallenge challenge)
          }
        }
      )
    }

  def hookForm = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) NoPlaybanOrCurrent {
      env.forms.hookFilled(timeModeString = get("time")) map { html.setup.hook(_) }
    }
    else fuccess {
      Redirect(routes.Lobby.home + "#hook")
    }
  }

  private def hookResponse(hookId: String) =
    Ok(Json.obj(
      "ok" -> true,
      "hook" -> Json.obj("id" -> hookId))) as JSON

  def hook(uid: String) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    PostRateLimit(req.remoteAddress) {
      NoPlaybanOrCurrent {
        env.forms.hook(ctx).bindFromRequest.fold(
          err => negotiate(
            html = BadRequest(errorsAsJson(err).toString).fuccess,
            api = _ => BadRequest(errorsAsJson(err)).fuccess),
          config => (ctx.userId ?? Env.relation.api.fetchBlocking) flatMap {
            blocking =>
              env.processor.hook(config, uid, HTTPRequest sid req, blocking) map hookResponse recover {
                case e: IllegalArgumentException => BadRequest(Json.obj("error" -> e.getMessage)) as JSON
              }
          }
        )
      }
    }
  }

  def like(uid: String, gameId: String) = Open { implicit ctx =>
    PostRateLimit(ctx.req.remoteAddress) {
      NoPlaybanOrCurrent {
        env.forms.hookConfig flatMap { config =>
          GameRepo game gameId map {
            _.fold(config)(config.updateFrom)
          } flatMap { config =>
            (ctx.userId ?? Env.relation.api.fetchBlocking) flatMap { blocking =>
              env.processor.hook(config, uid, HTTPRequest sid ctx.req, blocking) map hookResponse recover {
                case e: IllegalArgumentException => BadRequest(Json.obj("error" -> e.getMessage)) as JSON
              }
            }
          }
        }
      }
    }
  }

  def filterForm = Open { implicit ctx =>
    env.forms.filterFilled map {
      case (form, filter) => html.setup.filter(form, filter)
    }
  }

  def filter = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    env.forms.filter(ctx).bindFromRequest.fold[Fu[Result]](
      f => fulogwarn(f.errors.toString) inject BadRequest(()),
      config => JsonOk(env.processor filter config inject config.render)
    )
  }

  def join(id: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game id) { game =>
      ???
    }
  }

  // def await(fullId: String, userId: Option[String]) = Open { implicit ctx =>
  //   OptionFuResult(GameRepo pov fullId) { pov =>
  //     pov.game.started.fold(
  //       Redirect(routes.Round.player(pov.fullId)).fuccess,
  //       Env.api.roundApi.player(pov, lila.api.Mobile.Api.currentVersion) zip
  //         (userId ?? UserRepo.named) flatMap {
  //           case (data, user) => PreventTheft(pov) {
  //             Ok(html.setup.await(
  //               pov,
  //               data,
  //               env.friendConfigMemo get pov.game.id,
  //               user)).fuccess
  //           }
  //         }
  //     )
  //   }
  // }

  def validateFen = Open { implicit ctx =>
    get("fen") flatMap ValidFen(getBool("strict")) match {
      case None    => BadRequest.fuccess
      case Some(v) => Ok(html.game.miniBoard(v.fen, v.color.name)).fuccess
    }
  }

  private def process[A](form: Context => Form[A])(op: A => BodyContext[_] => Fu[(Pov, Call)]) =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      form(ctx).bindFromRequest.fold(
        f => negotiate(
          html = Lobby.renderHome(Results.BadRequest),
          api = _ => fuccess(BadRequest(errorsAsJson(f)))
        ),
        config => op(config)(ctx) flatMap {
          case (pov, call) => negotiate(
            html = fuccess(redirectPov(pov, call)),
            api = apiVersion => Env.api.roundApi.player(pov, apiVersion) map { data =>
              Created(data) as JSON
            }
          )
        }
      )
    }

  private def redirectPov(pov: Pov, call: Call)(implicit ctx: Context, req: RequestHeader) =
    if (ctx.isAuth) Redirect(call)
    else Redirect(call) withCookies LilaCookie.cookie(
      AnonCookie.name,
      pov.playerId,
      maxAge = AnonCookie.maxAge.some,
      httpOnly = false.some)
}
