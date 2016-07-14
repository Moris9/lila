package lila.plan

import scala.concurrent.duration._
import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    bus: lila.common.Bus,
    scheduler: lila.common.Scheduler) {

  val stripePublicKey = config getString "stripe.keys.public"

  private val CollectionPatron = config getString "collection.patron"
  private val CollectionCharge = config getString "collection.charge"

  private lazy val patronColl = db(CollectionPatron)

  private lazy val stripeClient = new StripeClient(StripeClient.Config(
    endpoint = config getString "stripe.endpoint",
    publicKey = stripePublicKey,
    secretKey = config getString "stripe.keys.secret"))

  lazy val api = new PlanApi(
    stripeClient,
    patronColl = patronColl,
    chargeColl = db(CollectionCharge),
    bus)

  private lazy val webhookHandler = new WebhookHandler(api)

  private lazy val expiration = new Expiration(patronColl)

  scheduler.future(10 seconds, "Expire patron plans") {
    expiration.run
  }

  def webhook = webhookHandler.apply _
}

object Env {

  lazy val current: Env = "plan" boot new Env(
    config = lila.common.PlayApp loadConfig "plan",
    db = lila.db.Env.current,
    bus = lila.common.PlayApp.system.lilaBus,
    scheduler = lila.common.PlayApp.scheduler)
}
