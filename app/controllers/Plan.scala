package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.{ EmailAddress, HTTPRequest }
import lila.plan.StripeClient.StripeException
import lila.plan.{
  Cents,
  Checkout,
  CreateStripeSession,
  CustomerId,
  Freq,
  MonthlyCustomerInfo,
  NextUrls,
  OneTimeCustomerInfo,
  StripeCustomer
}
import lila.user.{ User => UserModel }
import views._

final class Plan(env: Env)(implicit system: akka.actor.ActorSystem) extends LilaController(env) {

  private val logger = lila.log("plan")

  def index =
    Open { implicit ctx =>
      pageHit
      ctx.me.fold(indexAnon) { me =>
        import lila.plan.PlanApi.SyncResult._
        env.plan.api.sync(me) flatMap {
          case ReloadUser => Redirect(routes.Plan.index).fuccess
          case Synced(Some(patron), None) =>
            env.user.repo email me.id flatMap { email =>
              renderIndex(email, patron.some)
            }
          case Synced(Some(patron), Some(customer)) => indexPatron(me, patron, customer)
          case Synced(_, _)                         => indexFreeUser(me)
        }
      }
    }

  def list =
    Open { implicit ctx =>
      ctx.me.fold(Redirect(routes.Plan.index).fuccess) { me =>
        import lila.plan.PlanApi.SyncResult._
        env.plan.api.sync(me) flatMap {
          case ReloadUser         => Redirect(routes.Plan.list).fuccess
          case Synced(Some(_), _) => indexFreeUser(me)
          case _                  => Redirect(routes.Plan.index).fuccess
        }
      }
    }

  private def indexAnon(implicit ctx: Context) = renderIndex(email = none, patron = none)

  private def indexFreeUser(me: UserModel)(implicit ctx: Context) =
    env.user.repo email me.id flatMap { email =>
      renderIndex(email, patron = none)
    }

  private def renderIndex(email: Option[EmailAddress], patron: Option[lila.plan.Patron])(implicit
      ctx: Context
  ): Fu[Result] =
    for {
      recentIds <- env.plan.api.recentChargeUserIds
      bestIds   <- env.plan.api.topPatronUserIds
      _         <- env.user.lightUserApi preloadMany { recentIds ::: bestIds }
      pricing   <- env.plan.priceApi.pricingFor(myGeoLocale)
    } yield Ok(
      html.plan.index(
        stripePublicKey = env.plan.stripePublicKey,
        email = email,
        patron = patron,
        recentIds = recentIds,
        bestIds = bestIds,
        pricing = pricing
      )
    )

  private def indexPatron(me: UserModel, patron: lila.plan.Patron, customer: StripeCustomer)(implicit
      ctx: Context
  ) =
    env.plan.api.customerInfo(me, customer) flatMap {
      case Some(info: MonthlyCustomerInfo) =>
        Ok(html.plan.indexStripe(me, patron, info, stripePublicKey = env.plan.stripePublicKey)).fuccess
      case Some(info: OneTimeCustomerInfo) =>
        renderIndex(info.customer.email map EmailAddress.apply, patron.some)
      case None =>
        env.user.repo email me.id flatMap { email =>
          renderIndex(email, patron.some)
        }
    }

  private def myGeoLocale(implicit ctx: Context) =
    // "FR".some
    env.security
      .geoIP(HTTPRequest.ipAddress(ctx.req))
      .flatMap(_.countryCode)
      .flatMap { code => scala.util.Try(new java.util.Locale("", code)).toOption }
      .filter(env.plan.currencyApi.hasCurrency)
      .orElse(ctx.lang.locale.some)
      .filter(env.plan.currencyApi.hasCurrency)
      .getOrElse(env.plan.currencyApi.US)

  def features =
    Open { implicit ctx =>
      pageHit
      fuccess {
        html.plan.features()
      }
    }

  def switch =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      lila.plan.Switch.form
        .bindFromRequest()
        .fold(
          _ => funit,
          data => env.plan.api.switch(me, data.cents)
        ) inject Redirect(routes.Plan.index)
    }

  def cancel =
    AuthBody { _ => me =>
      env.plan.api.cancel(me) inject Redirect(routes.Plan.index)
    }

  def thanks =
    Open { implicit ctx =>
      // wait for the payment data from stripe or paypal
      lila.common.Future.delay(2.seconds) {
        ctx.me ?? env.plan.api.userPatron flatMap { patron =>
          patron ?? env.plan.api.patronCustomer map { customer =>
            Ok(html.plan.thanks(patron, customer))
          }
        }
      }
    }

  def webhook =
    Action.async(parse.json) { req =>
      env.plan.webhookHandler(req.body) map { _ =>
        Ok("kthxbye")
      }
    }

  def badStripeSession[A: Writes](err: A) = BadRequest(jsonError(err))
  def badStripeApiCall: PartialFunction[Throwable, Result] = { case e: StripeException =>
    logger.error("Plan.stripeCheckout", e)
    badStripeSession("Stripe API call failed")
  }

  private def createStripeSession(checkout: Checkout, customerId: CustomerId) =
    env.plan.api
      .createSession(
        CreateStripeSession(
          customerId,
          checkout,
          NextUrls(
            cancel = s"${env.net.baseUrl}${routes.Plan.index}",
            success = s"${env.net.baseUrl}${routes.Plan.thanks}"
          )
        )
      )
      .map(session => JsonOk(Json.obj("session" -> Json.obj("id" -> session.id.value))))
      .recover(badStripeApiCall)

  def switchStripePlan(user: UserModel, cents: Cents) = {
    env.plan.api
      .switch(user, cents)
      .inject(JsonOk(Json.obj("switch" -> Json.obj("cents" -> cents.value))))
      .recover(badStripeApiCall)
  }

  private val StripeRateLimit = lila.memo.RateLimit.composite[lila.common.IpAddress](
    key = "stripe.checkout.ip",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 8, 10.minute),
    ("slow", 40, 1.day)
  )

  def stripeCheckout =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      StripeRateLimit(HTTPRequest ipAddress req) {
        lila.plan.Checkout.form
          .bindFromRequest()
          .fold(
            err => {
              logger.info(s"Plan.stripeCheckout 400: $err")
              badStripeSession(err.toString).fuccess
            },
            checkout =>
              env.plan.api.userCustomer(me) flatMap {
                case Some(customer) if checkout.freq == Freq.Onetime =>
                  createStripeSession(checkout, customer.id)
                case Some(customer) if customer.firstSubscription.isDefined =>
                  switchStripePlan(me, checkout.amount)
                case _ =>
                  env.plan.api
                    .makeCustomer(me, checkout)
                    .flatMap(customer => createStripeSession(checkout, customer.id))
              }
          )
      }(rateLimitedFu)
    }

  def updatePayment =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      StripeRateLimit(HTTPRequest ipAddress req) {
        env.plan.api.userCustomer(me) flatMap {
          _.flatMap(_.firstSubscription) ?? { sub =>
            env.plan.api
              .createPaymentUpdateSession(
                sub,
                NextUrls(
                  cancel = s"${env.net.baseUrl}${routes.Plan.index}",
                  success =
                    s"${env.net.baseUrl}${routes.Plan.updatePaymentCallback}?session={CHECKOUT_SESSION_ID}"
                )
              )
              .map(session => JsonOk(Json.obj("session" -> Json.obj("id" -> session.id.value))))
              .recover(badStripeApiCall)
          }
        }
      }(rateLimitedFu)
    }

  def updatePaymentCallback =
    AuthBody { implicit ctx => me =>
      get("session") ?? { session =>
        env.plan.api.userCustomer(me) flatMap {
          _.flatMap(_.firstSubscription) ?? { sub =>
            env.plan.api.updatePayment(sub, session) inject Redirect(routes.Plan.index)
          }
        }
      }
    }

  def payPalIpn =
    Action.async { implicit req =>
      import lila.plan.Patron.PayPal
      lila.plan.PlanForm.ipn
        .bindFromRequest()
        .fold(
          err => {
            if (err.errors("txn_type").nonEmpty) {
              logger.debug(s"Plan.payPalIpn ignore txn_type = ${err.data get "txn_type"}")
              fuccess(Ok)
            } else {
              logger.error(s"Plan.payPalIpn invalid data ${err.toString}")
              fuccess(BadRequest)
            }
          },
          ipn =>
            env.plan.api.onPaypalCharge(
              userId = ipn.userId,
              email = ipn.email map PayPal.Email.apply,
              subId = ipn.subId map PayPal.SubId.apply,
              cents = lila.plan.Cents(ipn.grossCents),
              name = ipn.name,
              txnId = ipn.txnId,
              country = ipn.country,
              ip = lila.common.HTTPRequest.ipAddress(req).value,
              key = get("key", req) | "N/A"
            ) inject Ok
        )
    }
}
