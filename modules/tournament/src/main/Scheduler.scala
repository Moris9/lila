package lila.tournament

import akka.actor._
import org.joda.time.DateTime
import scala.concurrent.duration._

import actorApi._

private[tournament] final class Scheduler(api: TournamentApi) extends Actor {

  import Schedule.Freq._
  import Schedule.Speed._

  private val bulletHour = 17
  private val blitzHour = 18
  private val slowHour = 20

  def receive = {

    case ScheduleNow => TournamentRepo.scheduled.map(_.map(_.schedule).flatten) foreach { dbScheds =>

      val rightNow = DateTime.now
      val today = rightNow.withTimeAtStartOfDay
      val lastDayOfMonth = today.dayOfMonth.withMaximumValue
      val lastFridayOfCurrentMonth = lastDayOfMonth.minusDays((lastDayOfMonth.getDayOfWeek + 1) % 7)
      val nextFriday = today.plusDays((13 - today.getDayOfWeek) % 7)
      val nextHourDate = rightNow plusHours 1
      val nextHour = nextHourDate.getHourOfDay

      val scheds = List(
        Schedule(Monthly, Bullet, at(lastFridayOfCurrentMonth, bulletHour)),
        Schedule(Monthly, Blitz, at(lastFridayOfCurrentMonth, blitzHour)),
        Schedule(Monthly, Slow, at(lastFridayOfCurrentMonth, slowHour)),
        Schedule(Weekly, Bullet, at(nextFriday, bulletHour)),
        Schedule(Weekly, Blitz, at(nextFriday, blitzHour)),
        Schedule(Weekly, Slow, at(nextFriday, slowHour)),
        Schedule(Daily, Bullet, at(today, bulletHour)),
        Schedule(Daily, Blitz, at(today, blitzHour)),
        Schedule(Daily, Slow, at(today, slowHour)),
        Schedule(Hourly, Bullet, at(nextHourDate, nextHour)),
        Schedule(Hourly, Blitz, at(nextHourDate, nextHour, 30))
      ).foldLeft(List[Schedule]()) {
          case (scheds, sched) if sched.at.isBeforeNow      => scheds
          case (scheds, sched) if overlaps(sched, dbScheds) => scheds
          case (scheds, sched) if overlaps(sched, scheds)   => scheds
          case (scheds, sched)                              => sched :: scheds
        }

      scheds foreach api.createScheduled
    }
  }

  private def endsAt(s: Schedule) = s.at plus ((~Schedule.durationFor(s)).toLong * 60 * 1000)
  private def interval(s: Schedule) = new org.joda.time.Interval(s.at, endsAt(s))
  private def overlaps(s: Schedule, ss: Seq[Schedule]) = ss exists { s2 =>
    interval(s) overlaps interval(s2)
  }

  private def at(day: DateTime, hour: Int, minute: Int = 0) =
    day withHourOfDay hour withMinuteOfHour minute withSecondOfMinute 0 withMillisOfSecond 0
}
