package lila.api

import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }

import lila.common.HTTPRequest
import lila.pref.Pref
import lila.user.{ Me, MyId, User, UserContext }
import lila.notify.Notification.UnreadCount
import lila.oauth.{ OAuthScope, TokenScopes }

object context:
  export lila.api.{ AnyContext, BodyContext }
  export lila.api.{ WebContext, WebBodyContext }
  export lila.api.{ OAuthContext, OAuthBodyContext }
  export lila.api.{ MinimalContext, MinimalBodyContext }
  export lila.api.PageContext
  given (using ctx: AnyContext): Option[lila.user.Me]    = ctx.me
  given (using ctx: AnyContext): Option[lila.user.Me.Id] = ctx.meId
  given (using page: PageContext): AnyContext            = page.ctx
  given (using page: PageContext): WebContext =
    WebContext(page.ctx.req, page.ctx.lang, UserContext(page.me, false, page.impersonatedBy), page.ctx.pref)

/* data necessary to render the lichess website layout */
case class PageData(
    teamNbRequests: Int,
    nbChallenges: Int,
    nbNotifications: UnreadCount,
    hasClas: Boolean,
    inquiry: Option[lila.mod.Inquiry],
    nonce: Option[Nonce],
    error: Boolean = false
)

object PageData:

  def anon(nonce: Option[Nonce]) =
    PageData(
      teamNbRequests = 0,
      nbChallenges = 0,
      nbNotifications = UnreadCount(0),
      hasClas = false,
      inquiry = none,
      nonce = nonce
    )

  def error(nonce: Option[Nonce]) = anon(nonce).copy(error = true)

trait AnyContext:
  val req: RequestHeader
  def lang: Lang
  def pref: Pref
  val me: Option[Me]
  val impersonatedBy: Option[User]
  val needsFp: Boolean
  export me.{ isDefined as isAuth, isEmpty as isAnon }
  def meId: Option[MyId]             = me.map(_.meId)
  def is[U: UserIdOf](u: U): Boolean = me.exists(_ is u)
  inline def user: Option[User]      = Me raw me
  def userId: Option[UserId]         = user.map(_.id)
  def username: Option[UserName]     = user.map(_.username)
  def isBot                          = me.exists(_.isBot)
  def noBot                          = !isBot
  def troll                          = user.exists(_.marks.troll)
  def kid                            = user.exists(_.kid)
  def noKid                          = !kid
  def isAppealUser                   = me.exists(_.enabled.no)
  def ip                             = HTTPRequest ipAddress req
  val scopes: TokenScopes            = TokenScopes(Nil)
  def isMobile                       = scopes.has(_.Web.Mobile)
  def isWebAuth: Boolean             = false
  def isOAuthAuth: Boolean           = false
  lazy val blind                     = req.cookies.get(ApiConfig.blindCookie.name).exists(_.value.nonEmpty)
  def noBlind                        = !blind
  lazy val mobileApiVersion          = Mobile.Api requestVersion req
  def isMobileApi                    = mobileApiVersion.isDefined
  def flash(name: String): Option[String] = req.flash get name

final class PageContext(val ctx: AnyContext, val data: PageData) extends AnyContext:
  export data.{ teamNbRequests, nbChallenges, nbNotifications, nonce, hasClas }
  export ctx.*

  lazy val isMobileBrowser = HTTPRequest isMobile req

  def zoom: Int = {
    def oldZoom = req.session get "zoom2" flatMap (_.toIntOption) map (_ - 100)
    req.cookies get "zoom" map (_.value) flatMap (_.toIntOption) orElse oldZoom filter (0 <=) filter (100 >=)
  } | 85

trait BodyContext[A] extends AnyContext:
  val body: Request[A]

/* Able to render a lichess page with a layout. Might be authenticated with cookie session */
class WebContext(
    val req: RequestHeader,
    val lang: Lang,
    val userContext: UserContext,
    val pref: Pref
) extends AnyContext:

  export userContext.{ me, impersonatedBy, needsFp }

  override def isWebAuth = isAuth

  def withLang(l: Lang) = WebContext(req, l, userContext, pref)

/* Able to render a lichess page with a layout. Might be authenticated with cookie session */
final class WebBodyContext[A](
    val body: Request[A],
    lang: Lang,
    userContext: UserContext,
    pref: Pref
) extends WebContext(body, lang, userContext, pref)
    with BodyContext[A]:
  override def withLang(l: Lang) = WebBodyContext(body, l, userContext, pref)

/* Cannot render a lichess page. Might be authenticated oauth and have scopes */
class OAuthContext(
    val req: RequestHeader,
    val lang: Lang,
    val me: Option[Me],
    val pref: Pref,
    override val scopes: TokenScopes
) extends AnyContext:
  val impersonatedBy                = none
  val needsFp                       = false
  override def isOAuthAuth: Boolean = me.isDefined

final class OAuthBodyContext[A](
    val body: Request[A],
    lang: Lang,
    me: Option[Me],
    pref: Pref,
    override val scopes: TokenScopes
) extends OAuthContext(body, lang, me, pref, scopes)
    with BodyContext[A]:
  def scoped = me.map(OAuthScope.Scoped(_, scopes))

/* Cannot render a lichess page. Cannot be authenticated. */
class MinimalContext(val req: RequestHeader) extends AnyContext:
  lazy val lang      = lila.i18n.I18nLangPicker(req)
  val me             = none
  val impersonatedBy = none
  val needsFp        = false
  lazy val pref      = lila.pref.RequestPref.fromRequest(req)

final class MinimalBodyContext[A](val body: Request[A]) extends MinimalContext(body) with BodyContext[A]
