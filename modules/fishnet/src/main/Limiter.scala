package lila.fishnet

import scala.concurrent.duration._
import reactivemongo.bson._

import lila.common.IpAddress
import lila.db.dsl._

private final class Limiter(
    analysisColl: Coll,
    requesterApi: lila.analyse.RequesterApi
) {

  def apply(sender: Work.Sender): Fu[Boolean] =
    concurrentCheck(sender) flatMap {
      case false => fuccess(false)
      case true => perDayCheck(sender)
    }

  private val RequestLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 50,
    duration = 20 hours,
    name = "request analysis per IP",
    key = "request_analysis.ip"
  )

  private def concurrentCheck(sender: Work.Sender) = sender match {
    case Work.Sender(_, _, mod, system) if (mod || system) => fuccess(true)
    case Work.Sender(Some(userId), _, _, _) => !analysisColl.exists($doc(
      "sender.userId" -> userId
    ))
    case Work.Sender(_, Some(ip), _, _) => !analysisColl.exists($doc(
      "sender.ip" -> ip
    ))
    case _ => fuccess(false)
  }

  private val maxPerDay = 30

  private def perDayCheck(sender: Work.Sender) = sender match {
    case Work.Sender(_, _, mod, system) if mod || system => fuccess(true)
    case Work.Sender(Some(userId), _, _, _) => requesterApi.countToday(userId) map (_ < maxPerDay)
    case Work.Sender(_, Some(ip), _, _) => fuccess {
      RequestLimitPerIP(ip, cost = 1)(true)
    }
    case _ => fuccess(false)
  }
}
