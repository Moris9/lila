package controllers

import lila.app._
import lila.common.LilaCookie
import lila.user.{ Context, HeaderContext, BodyContext, User ⇒ UserModel }
import lila.security.{ Permission, Granter }

import scalaz.Zero
import play.api.mvc._, Results._
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.data.Form
import play.api.templates.Html
import play.api.http._
import play.api.libs.json.{ Json, JsValue, Writes }

private[controllers] trait LilaController
    extends Controller
    with ContentTypes
    with RequestGetter
    with ResponseWriter
    with Results {

  protected implicit val LilaResultZero = new Zero[Result] {
    val zero = Results.NotFound
  }
  protected implicit val LilaPlainResultZero = new Zero[PlainResult] {
    val zero = Results.NotFound
  }
  protected implicit val LilaHtmlZero = new Zero[Html] {
    val zero = Html("")
  }

  protected implicit final class LilaPimpedResult(result: Result) {
    def fuccess = scala.concurrent.Future successful result
  }

  protected implicit def LilaHtmlToResult(content: Html): Result = Ok(content)

  protected implicit def LilaFunitToResult(funit: Funit): Fu[Result] = funit inject Ok("ok")

  override implicit def lang(implicit req: RequestHeader) =
    Env.i18n.pool lang req

  protected def Socket[A: FrameFormatter](f: Context ⇒ Fu[(Iteratee[A, _], Enumerator[A])]) =
    WebSocket.async[A] { req ⇒ reqToCtx(req) flatMap f }

  protected def Open(f: Context ⇒ Fu[Result]): Action[AnyContent] =
    Open(BodyParsers.parse.anyContent)(f)

  protected def Open[A](p: BodyParser[A])(f: Context ⇒ Fu[Result]): Action[A] =
    Action(p)(req ⇒ Async(reqToCtx(req) flatMap f))

  protected def OpenBody(f: BodyContext ⇒ Fu[Result]): Action[AnyContent] =
    OpenBody(BodyParsers.parse.anyContent)(f)

  protected def OpenBody[A](p: BodyParser[A])(f: BodyContext ⇒ Fu[Result]): Action[A] =
    Action(p)(req ⇒ Async(reqToCtx(req) flatMap f))

  protected def Auth(f: Context ⇒ UserModel ⇒ Fu[Result]): Action[AnyContent] =
    Auth(BodyParsers.parse.anyContent)(f)

  protected def Auth[A](p: BodyParser[A])(f: Context ⇒ UserModel ⇒ Fu[Result]): Action[A] =
    Action(p)(req ⇒ Async {
      reqToCtx(req) flatMap { ctx ⇒
        ctx.me.fold(authenticationFailed(ctx.req).fuccess)(me ⇒ f(ctx)(me))
      }
    })

  protected def AuthBody(f: BodyContext ⇒ UserModel ⇒ Fu[Result]): Action[AnyContent] =
    AuthBody(BodyParsers.parse.anyContent)(f)

  protected def AuthBody[A](p: BodyParser[A])(f: BodyContext ⇒ UserModel ⇒ Fu[Result]): Action[A] =
    Action(p)(req ⇒ Async {
      reqToCtx(req) flatMap { ctx ⇒
        ctx.me.fold(authenticationFailed(ctx.req).fuccess)(me ⇒ f(ctx)(me))
      }
    })

  protected def Secure(perm: Permission.type ⇒ Permission)(f: Context ⇒ UserModel ⇒ Fu[Result]): Action[AnyContent] =
    Secure(perm(Permission))(f)

  protected def Secure(perm: Permission)(f: Context ⇒ UserModel ⇒ Fu[Result]): Action[AnyContent] =
    Secure(BodyParsers.parse.anyContent)(perm)(f)

  protected def Secure[A](p: BodyParser[A])(perm: Permission)(f: Context ⇒ UserModel ⇒ Fu[Result]): Action[A] =
    Auth(p) { implicit ctx ⇒
      me ⇒
        isGranted(perm).fold(f(ctx)(me), fuccess(authorizationFailed(ctx.req)))
    }

  protected def Firewall[A <: Result](a: ⇒ Fu[A])(implicit ctx: Context): Fu[Result] =
    Env.security.firewall.accepts(ctx.req) flatMap {
      _ fold (a, {
        Env.security.firewall.logBlock(ctx.req)
        fuccess { Redirect(routes.Lobby.home()) }
      })
    }

  // protected def NoEngine[A <: Result](a: ⇒ A)(implicit ctx: Context): Result =
  //   ctx.me.fold(false)(_.engine).fold(Forbidden(views.html.site.noEngine()), a)

  protected def JsonOk[A: Writes](fua: Fu[A]) = fua map { a ⇒
    Ok(Json toJson a) as JSON
  }

  protected def JsonOptionOk[A: Writes](fua: Fu[Option[A]])(implicit ctx: Context) = fua flatMap {
    _.fold(notFound(ctx))(a ⇒ fuccess(Ok(Json toJson a) as JSON))
  }

  protected def JsOk(fua: Fu[String], headers: (String, String)*) =
    fua map { a ⇒ Ok(a) as JAVASCRIPT withHeaders (headers: _*) }

  // protected def ValidOk(valid: Valid[Unit]): Result = valid.fold(
  //   e ⇒ BadRequest(e.shows),
  //   _ ⇒ Ok("ok")
  // )

  // protected def ValidFuk(valid: Fu[Valid[Unit]]): Result = ValidOk(valid.unsafePerformFu)

  protected def FormResult[A](form: Form[A])(op: A ⇒ Fu[Result])(implicit req: Request[_]): Fu[Result] =
    form.bindFromRequest.fold(
      form ⇒ fuccess(BadRequest(form.errors mkString "\n")),
      op)

  protected def FormFuResult[A, B: Writeable: ContentTypeOf](form: Form[A])(err: Form[A] ⇒ Fu[B])(op: A ⇒ Fu[Result])(implicit req: Request[_]) =
    form.bindFromRequest.fold(
      form ⇒ err(form) map { BadRequest(_) },
      data ⇒ op(data)
    )

  protected def FuRedirect(fua: Fu[Call]) = fua map { Redirect(_) }

  protected def OptionOk[A, B: Writeable: ContentTypeOf](fua: Fu[Option[A]])(op: A ⇒ B)(implicit ctx: Context): Fu[Result] =
    OptionFuOk(fua) { a ⇒ fuccess(op(a)) }

  protected def OptionFuOk[A, B: Writeable: ContentTypeOf](fua: Fu[Option[A]])(op: A ⇒ Fu[B])(implicit ctx: Context) =
    fua flatMap { _.fold(notFound(ctx))(a ⇒ op(a) map { Ok(_) }) }

  // protected def FuptionFuResult[A](fua: Fu[Option[A]])(op: A ⇒ Fu[Result])(implicit ctx: Context) =
  //   fua flatMap { _.fold(io(notFound(ctx)))(op) } unsafePerformFu

  // protected def FuptionRedirect[A](fua: Fu[Option[A]])(op: A ⇒ Call)(implicit ctx: Context) =
  //   fua map {
  //     _.fold(notFound(ctx))(a ⇒ Redirect(op(a)))
  //   } unsafePerformFu

  // protected def FuptionFuRedirect[A](fua: Fu[Option[A]])(op: A ⇒ Fu[Call])(implicit ctx: Context) =
  //   (fua flatMap {
  //     _.fold(io(notFound(ctx)))(a ⇒ op(a) map { b ⇒ Redirect(b) })
  //   }: Fu[Result]).unsafePerformFu

  protected def OptionFuRedirectUrl[A](fua: Fu[Option[A]])(op: A ⇒ Fu[String])(implicit ctx: Context) =
    fua flatMap {
      _.fold(notFound(ctx))(a ⇒ op(a) map { b ⇒ Redirect(b) })
    }

  protected def OptionResult[A](fua: Fu[Option[A]])(op: A ⇒ Result)(implicit ctx: Context) =
    OptionFuResult(fua) { a ⇒ fuccess(op(a)) }

  protected def OptionFuResult[A](fua: Fu[Option[A]])(op: A ⇒ Fu[Result])(implicit ctx: Context) =
    fua flatMap { _.fold(notFound(ctx))(a ⇒ op(a)) }

  protected def notFound(implicit ctx: Context): Fu[Result] =
    Lobby handleNotFound ctx

  // protected def todo = Open { implicit ctx ⇒
  //   NotImplemented(views.html.site.todo())
  // }

  protected def isGranted(permission: Permission.type ⇒ Permission)(implicit ctx: Context): Boolean =
    isGranted(permission(Permission))

  protected def isGranted(permission: Permission)(implicit ctx: Context): Boolean =
    ctx.me.zmap(Granter(permission))

  protected def authenticationFailed(implicit req: RequestHeader): Result =
    Redirect(routes.Auth.signup) withCookies LilaCookie.session(Env.security.api.AccessUri, req.uri)

  protected def authorizationFailed(req: RequestHeader): Result =
    Forbidden("no permission")

  protected def reqToCtx(req: Request[_]): Fu[BodyContext] =
    Env.security.api restoreUser req map { user ⇒
      setOnline(user)
      Context(req, user)
    }

  protected def reqToCtx(req: RequestHeader): Fu[HeaderContext] =
    Env.security.api restoreUser req map { user ⇒
      setOnline(user)
      Context(req, user)
    }

  private def setOnline(user: Option[UserModel]) {
    user foreach Env.user.setOnline
  }

  private val maxPage = 40
  protected def Reasonable(page: Int)(result: ⇒ Fu[Result]): Fu[Result] =
    (page < maxPage).fold(result, BadRequest("resource too old").fuccess)
}
