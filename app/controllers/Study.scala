package controllers

import play.api.http.ContentTypes
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest
import lila.study.Order
import views._

object Study extends LilaController {

  type ListUrl = (lila.user.User.ID, String) => Call

  private def env = Env.study

  def byOwnerDefault(username: String, page: Int) = byOwner(username, Order.default.key, page)

  def byOwner(username: String, order: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(lila.user.UserRepo named username) { owner =>
      env.pager.byOwnerForUser(owner.id, ctx.me, Order(order), page) map { pag =>
        html.study.byOwner(pag, Order(order), owner)
      }
    }
  }

  def byOwnerPublic(username: String, order: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(lila.user.UserRepo named username) { owner =>
      env.pager.byOwnerPublicForUser(owner.id, ctx.me, Order(order), page) map { pag =>
        html.study.byOwnerPublic(pag, Order(order), owner)
      }
    }
  }

  def byOwnerPrivate(username: String, order: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(lila.user.UserRepo named username) { owner =>
      env.pager.byOwnerPrivateForUser(owner.id, ctx.me, Order(order), page) map { pag =>
        html.study.byOwnerPrivate(pag, Order(order), owner)
      }
    }
  }

  def byMember(username: String, order: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(lila.user.UserRepo named username) { member =>
      env.pager.byMemberForUser(member.id, ctx.me, Order(order), page) map { pag =>
        html.study.byMember(pag, Order(order), member)
      }
    }
  }

  def byLikes(username: String, order: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(lila.user.UserRepo named username) { user =>
      env.pager.byLikesForUser(user.id, ctx.me, Order(order), page) map { pag =>
        html.study.byLikes(pag, Order(order), user)
      }
    }
  }

  def show(id: String) = Open { implicit ctx =>
    val query = get("chapterId").fold(env.api byIdWithChapter id) { chapterId =>
      env.api.byIdWithChapter(id, chapterId)
    }
    OptionFuResult(query) {
      case lila.study.Study.WithChapter(study, chapter) => CanViewResult(study) {
        env.chapterRepo.orderedMetadataByStudy(study.id) flatMap { chapters =>
          if (HTTPRequest isSynchronousHttp ctx.req) env.studyRepo.incViews(study)
          val setup = chapter.setup
          val initialFen = chapter.root.fen
          val pov = UserAnalysis.makePov(initialFen.value.some, setup.variant)
          Env.round.jsonView.userAnalysisJson(pov, ctx.pref, setup.orientation, owner = false) zip
            Env.chat.api.userChat.find(study.id) zip
            env.jsonView(study, chapters, chapter, ctx.me) zip
            env.version(id) flatMap {
              case (((baseData, chat), studyJson), sVersion) =>
                import lila.socket.tree.Node.partitionTreeJsonWriter
                val analysis = baseData ++ Json.obj(
                  "treeParts" -> partitionTreeJsonWriter.writes(lila.study.TreeBuilder(chapter.root)))
                val data = lila.study.JsonView.JsData(
                  study = studyJson,
                  analysis = analysis,
                  chat = lila.chat.JsonView(chat))
                negotiate(
                  html = Ok(html.study.show(study, data, sVersion)).fuccess,
                  api = _ => Ok(Json.obj(
                    "study" -> data.study,
                    "analysis" -> data.analysis)).fuccess
                )
            }
        }
      }
    } map NoCache
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    get("sri") ?? { uid =>
      env.api byId id flatMap {
        _.filter(canView) ?? { study =>
          env.socketHandler.join(
            studyId = id,
            uid = lila.socket.Socket.Uid(uid),
            user = ctx.me,
            owner = ctx.userId.exists(study.isOwner))
        }
      }
    }
  }

  def create = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      lila.study.DataForm.form.bindFromRequest.fold(
        err => Redirect(routes.Study.byOwnerDefault(me.username)).fuccess,
        data => env.api.create(data, me) map { sc =>
          Redirect(routes.Study.show(sc.study.id))
        })
  }

  def delete(id: String) = Auth { implicit ctx =>
    me =>
      env.api.byId(id) flatMap { study =>
        study.filter(_ isOwner me.id) ?? env.api.delete
      } inject Redirect(routes.Study.byOwnerDefault(me.username))
  }

  def pgn(id: String) = Open { implicit ctx =>
    OptionFuResult(env.api byId id) { study =>
      CanViewResult(study) {
        env.pgnDump(study) map { pgns =>
          Ok(pgns.mkString("\n\n\n")).withHeaders(
            CONTENT_TYPE -> ContentTypes.TEXT,
            CONTENT_DISPOSITION -> ("attachment; filename=" + (env.pgnDump filename study)))
        }
      }
    }
  }

  private def CanViewResult(study: lila.study.Study)(f: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (canView(study)) f
    else fuccess(Unauthorized(html.study.restricted(study)))

  private def canView(study: lila.study.Study)(implicit ctx: lila.api.Context) =
    study.isPublic || ctx.userId.exists(study.members.contains)
}
