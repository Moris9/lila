package controllers

import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest
import lila.forum.CategRepo
import play.api.libs.json._
import views._

object ForumTopic extends LilaController with ForumController {

  private val CreateRateLimit = new lila.memo.RateLimit(2, 5 minutes,
    name = "forum create topic",
    key = "forum.topic")

  def form(categSlug: String) = Open { implicit ctx =>
    NotForKids {
      CategGrantWrite(categSlug) {
        OptionFuOk(CategRepo bySlug categSlug) { categ =>
          forms.anyCaptcha map { html.forum.topic.form(categ, forms.topic, _) }
        }
      }
    }
  }

  def create(categSlug: String) = OpenBody { implicit ctx =>
    CreateRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
      CategGrantWrite(categSlug) {
        implicit val req = ctx.body
        OptionFuResult(CategRepo bySlug categSlug) { categ =>
          forms.topic.bindFromRequest.fold(
            err => forms.anyCaptcha map { captcha =>
              BadRequest(html.forum.topic.form(categ, err, captcha))
            },
            data => topicApi.makeTopic(categ, data) map { topic =>
              Redirect(routes.ForumTopic.show(categ.slug, topic.slug, 1))
            }
          )
        }
      }
    }
  }

  def show(categSlug: String, slug: String, page: Int) = Open { implicit ctx =>
    NotForKids {
      CategGrantRead(categSlug) {
        OptionFuOk(topicApi.show(categSlug, slug, page, ctx.troll)) {
          case (categ, topic, posts) => for {
            unsub <- ctx.userId ?? Env.timeline.status(s"forum:${topic.id}")
            form <- (!posts.hasNextPage && isGrantedWrite(categSlug) && topic.open) ?? forms.postWithCaptcha.map(_.some)
            canModCateg <- isGrantedMod(categ.slug)
          } yield html.forum.topic.show(categ, topic, posts, form, unsub, canModCateg = canModCateg)
        }
      }
    }
  }

  def close(categSlug: String, slug: String) = Auth { implicit ctx => me =>
    CategGrantMod(categSlug) {
      OptionFuRedirect(topicApi.show(categSlug, slug, 1, ctx.troll)) {
        case (categ, topic, pag) => topicApi.toggleClose(categ, topic, me) inject
          routes.ForumTopic.show(categSlug, slug, pag.nbPages)
      }
    }
  }

  def hide(categSlug: String, slug: String) = Secure(_.ModerateForum) { implicit ctx => me =>
    OptionFuRedirect(topicApi.show(categSlug, slug, 1, ctx.troll)) {
      case (categ, topic, pag) => topicApi.toggleHide(categ, topic, me) inject
        routes.ForumTopic.show(categSlug, slug, pag.nbPages)
    }
  }

  /**
   * Returns a list of the usernames of people participating in a forum topic conversation
   */
  def participants(topicId: String) = Auth { implicit ctx => me =>
    postApi.userIds(topicId) map { ids =>
      val usernames = Env.user.lightUserApi.getList(ids.sorted).map(_.name)
      Ok(Json.toJson(usernames))
    }
  }
}
