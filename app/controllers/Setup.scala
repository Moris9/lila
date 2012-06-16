package controllers

import lila._
import views._
import setup._
import http.{ Context, BodyContext }

import play.api.mvc.Call
import play.api.data.Form

import scalaz.effects._

object Setup extends LilaController with TheftPrevention {

  def forms = env.setup.formFactory
  def processor = env.setup.processor
  def friendConfigMemo = env.setup.friendConfigMemo
  def gameRepo = env.game.gameRepo
  def bookmarkApi = env.bookmark.api

  val aiForm = Open { implicit ctx ⇒
    IOk(forms.aiFilled map { html.setup.ai(_) })
  }

  val ai = process(forms.ai) { config ⇒
    implicit ctx ⇒
      processor ai config map { pov ⇒
        routes.Round.player(pov.fullId)
      }
  }

  val friendForm = Open { implicit ctx ⇒
    IOk(forms.friendFilled map { html.setup.friend(_) })
  }

  val friend = process(forms.friend) { config ⇒
    implicit ctx ⇒
      processor friend config map { pov ⇒
        routes.Setup.await(pov.fullId)
      }
  }

  val hookForm = Open { implicit ctx ⇒
    IOk(forms.hookFilled map { html.setup.hook(_) })
  }

  val hook = process(forms.hook) { config ⇒
    implicit ctx ⇒
      processor hook config map { hook ⇒
        routes.Lobby.hook(hook.ownerId)
      }
  }

  def await(fullId: String) = Open { implicit ctx ⇒
    IOptionResult(gameRepo pov fullId) { pov ⇒
      pov.game.started.fold(
        Redirect(routes.Round.player(pov.fullId)),
        PreventTheft(pov) {
          Ok(html.setup.await(
            pov,
            version(pov.gameId),
            friendConfigMemo get pov.game.id))
        }
      )
    }
  }

  def cancel(fullId: String) = Open { implicit ctx ⇒
    IOptionIORedirect(gameRepo pov fullId) { pov ⇒
      pov.game.started.fold(
        io(routes.Round.player(pov.fullId)),
        for {
          _ ← gameRepo remove pov.game.id
          _ ← bookmarkApi removeByGame pov.game
        } yield routes.Lobby.home
      )
    }
  }

  val api = Open { implicit ctx ⇒
    JsonIOk(processor.api)
  }

  private def process[A](form: Context ⇒ Form[A])(op: A ⇒ BodyContext ⇒ IO[Call]) =
    OpenBody { ctx ⇒
      implicit val req = ctx.body
      IORedirect(form(ctx).bindFromRequest.fold(
        f ⇒ putStrLn(f.errors.toString) map { _ ⇒ routes.Lobby.home },
        config ⇒ op(config)(ctx)
      ))
    }

  private def version(gameId: String): Int =
    env.round.socket blockingVersion gameId
}
