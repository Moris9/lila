package lila.plan

import org.joda.time.DateTime

case class Patron(
    _id: Patron.UserId,
    stripe: Option[Patron.Stripe] = none,
    payPal: Option[Patron.PayPal] = none,
    expiresAt: Option[DateTime] = none,
    lastLevelUp: DateTime) {

  def id = _id

  def userId = _id.value

  def canLevelUp = lastLevelUp isBefore DateTime.now.minusDays(25)

  def levelUpNow = copy(
    lastLevelUp = DateTime.now)

  def expireInOneMonth: Patron = copy(
    expiresAt = DateTime.now.plusMonths(1).plusDays(1).some)

  def expireInOneMonth(cond: Boolean): Patron =
    if (cond) expireInOneMonth
    else copy(expiresAt = none)

  def removeStripe = copy(
    stripe = none,
    expiresAt = none)

  def removePayPal = copy(
    payPal = none,
    expiresAt = none)

  def isDefined = stripe.isDefined || payPal.isDefined
}

object Patron {

  case class UserId(value: String) extends AnyVal

  case class Stripe(customerId: CustomerId)

  case class PayPal(
      email: Option[PayPal.Email],
      subId: Option[PayPal.SubId],
      lastCharge: DateTime) {
  }
  object PayPal {
    case class Email(value: String) extends AnyVal
    case class SubId(value: String) extends AnyVal
  }
}
