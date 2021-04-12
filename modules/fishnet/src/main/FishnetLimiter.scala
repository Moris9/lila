package lila.fishnet

import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.common.IpAddress
import lila.db.dsl._

final private class FishnetLimiter(
    analysisColl: Coll,
    requesterApi: lila.analyse.RequesterApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(sender: Work.Sender, ignoreConcurrentCheck: Boolean, ownGame: Boolean): Fu[Boolean] =
    (fuccess(ignoreConcurrentCheck) >>| concurrentCheck(sender)) flatMap {
      case false => fuFalse
      case true  => perDayCheck(sender)
    } flatMap { accepted =>
      (accepted ?? requesterApi.add(sender.userId, ownGame)) inject accepted
    }

  private val RequestLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 60,
    duration = 20 hours,
    key = "request_analysis.ip"
  )

  private def concurrentCheck(sender: Work.Sender) =
    sender match {
      case Work.Sender(_, _, mod, system) if mod || system => fuTrue
      case Work.Sender(userId, ip, _, _) =>
        !analysisColl.exists(
          $or(
            $doc("sender.ip"     -> ip),
            $doc("sender.userId" -> userId)
          )
        )
      case _ => fuFalse
    }

  private val maxPerDay  = 35
  private val maxPerWeek = 160

  private def perDayCheck(sender: Work.Sender) =
    sender match {
      case Work.Sender(_, _, mod, system) if mod || system => fuTrue
      case Work.Sender(userId, ip, _, _) =>
        def perUser =
          requesterApi.countTodayAndThisWeek(userId) map { case (daily, weekly) =>
            weekly < maxPerWeek &&
              daily < (if (weekly < maxPerWeek * 2 / 3) maxPerDay else maxPerDay * 2 / 3)
          }
        ip.fold(perUser) { ipAddress =>
          RequestLimitPerIP(ipAddress, cost = 1)(perUser)(fuccess(false))
        }
      case _ => fuFalse
    }
}
