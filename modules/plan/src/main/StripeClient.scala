package lila.plan

import play.api.libs.json._
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current

import lila.common.PimpedJson._
import lila.user.{ User, UserRepo }

private final class StripeClient(config: StripeClient.Config) {

  import StripeClient._
  import JsonHandlers._

  def createCustomer(user: User, plan: StripePlan, data: Checkout): Fu[StripeCustomer] =
    UserRepo email user.id flatMap { email =>
      postOne[StripeCustomer]("customers",
        'plan -> plan.id,
        'email -> data.email,
        'source -> data.source.value,
        'email -> email,
        'description -> user.id)
    }

  def createAnonCustomer(plan: StripePlan, data: Checkout): Fu[StripeCustomer] =
    postOne[StripeCustomer]("customers",
      'plan -> plan.id,
      'email -> data.email,
      'source -> data.source.value)

  def getCustomer(id: CustomerId): Fu[Option[StripeCustomer]] =
    getOne[StripeCustomer](s"customers/${id.value}")

  def createSubscription(customer: StripeCustomer, plan: StripePlan, source: Source): Fu[StripeSubscription] =
    postOne[StripeSubscription]("subscriptions",
      'customer -> customer.id,
      'plan -> plan.id,
      'source -> source.value)

  def updateSubscription(sub: StripeSubscription, plan: StripePlan, source: Option[Source]): Fu[StripeSubscription] =
    postOne[StripeSubscription](s"subscriptions/${sub.id}",
      'plan -> plan.id,
      'source -> source.map(_.value),
      'prorate -> false)

  def cancelSubscription(sub: StripeSubscription): Funit =
    deleteOne(s"subscriptions/${sub.id}")

  def getEvent(id: String): Fu[Option[JsObject]] =
    getOne[JsObject](s"events/$id")

  def getNextInvoice(customerId: CustomerId): Fu[Option[StripeInvoice]] =
    getOne[StripeInvoice](s"invoices/upcoming", 'customer -> customerId.value)

  def getPastInvoices(customerId: CustomerId): Fu[List[StripeInvoice]] =
    getList[StripeInvoice]("invoices", 'customer -> customerId.value)

  def getPlan(cents: Cents): Fu[Option[StripePlan]] =
    getOne[StripePlan](s"plans/${StripePlan.make(cents).id}")

  def makePlan(cents: Cents): Fu[StripePlan] =
    postOne[StripePlan]("plans",
      'id -> StripePlan.make(cents).id,
      'amount -> cents.value,
      'currency -> "usd",
      'interval -> "month",
      'name -> StripePlan.make(cents).name)

  private def getOne[A: Reads](url: String, queryString: (Symbol, Any)*): Fu[Option[A]] =
    get[A](url, queryString) map Some.apply recover {
      case _: NotFoundException => None
      case e: DeletedException => {
        play.api.Logger("stripe").warn(e.getMessage)
        None
      }
    }

  private def getList[A: Reads](url: String, queryString: (Symbol, Any)*): Fu[List[A]] =
    get[List[A]](url, queryString)(listReader[A])

  private def postOne[A: Reads](url: String, data: (Symbol, Any)*): Fu[A] = post[A](url, data)

  private def deleteOne(url: String, queryString: (Symbol, Any)*): Fu[Unit] = delete(url, queryString)

  private def get[A: Reads](url: String, queryString: Seq[(Symbol, Any)]): Fu[A] = {
    logger.info(s"GET $url ${debugInput(queryString)}")
    request(url).withQueryString(fixInput(queryString): _*).get() flatMap response[A]
  }

  private def post[A: Reads](url: String, data: Seq[(Symbol, Any)]): Fu[A] = {
    logger.info(s"POST $url ${debugInput(data)}")
    request(url).post(fixInput(data).toMap mapValues { Seq(_) }) flatMap response[A]
  }

  private def delete(url: String, queryString: Seq[(Symbol, Any)]): Fu[Unit] = {
    logger.info(s"DELETE $url ${debugInput(queryString)}")
    request(url).withQueryString(fixInput(queryString): _*).delete() map (_ => ())
  }

  private def request(url: String) =
    WS.url(s"${config.endpoint}/$url").withHeaders("Authorization" -> s"Bearer ${config.secretKey}")

  private def response[A: Reads](res: WSResponse): Fu[A] = res.status match {
    case 200 => (implicitly[Reads[A]] reads res.json).fold(
      errs => fufail {
        if (isDeleted(res.json)) new DeletedException(s"[stripe] Upstream resource was deleted: ${res.json}")
        else new Exception(s"[stripe] Can't parse ${res.json} --- $errs")
      },
      fuccess
    )
    case 404 => fufail { new NotFoundException(s"[stripe] Not found") }
    case x if x >= 400 && x < 500 => (res.json \ "error" \ "message").asOpt[String] match {
      case None        => fufail { new InvalidRequestException(Json stringify res.json) }
      case Some(error) => fufail { new InvalidRequestException(error) }
    }
    case status => fufail { new StatusException(s"[stripe] Response status: $status") }
  }

  private def isDeleted(js: JsValue): Boolean =
    (js.asOpt[JsObject] flatMap { o => (o \ "deleted").asOpt[Boolean] }) == Some(true)

  private def fixInput(in: Seq[(Symbol, Any)]): Seq[(String, String)] = (in map {
    case (sym, Some(x)) => Some(sym.name -> x.toString)
    case (sym, None)    => None
    case (sym, x)       => Some(sym.name -> x.toString)
  }).flatten

  private def listReader[A: Reads]: Reads[List[A]] = (__ \ "data").read[List[A]]

  private def debugInput(data: Seq[(Symbol, Any)]) = fixInput(data) map { case (k, v) => s"$k=$v" } mkString " "
}

object StripeClient {

  class StripeException(msg: String) extends Exception(msg)
  class DeletedException(msg: String) extends StripeException(msg)
  class StatusException(msg: String) extends StripeException(msg)
  class NotFoundException(msg: String) extends StatusException(msg)
  class InvalidRequestException(msg: String) extends StatusException(msg)

  case class Config(endpoint: String, publicKey: String, secretKey: String)
}
