package lila.tournament

import org.joda.time.DateTime

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    at: DateTime) {

  def name = s"${freq.toString} ${speed.toString}"

  def sameSpeed(other: Schedule) = speed == other.speed
}

object Schedule {

  sealed trait Freq {
    def name = toString.toLowerCase
  }
  object Freq {
    case object Hourly extends Freq
    case object Daily extends Freq
    case object Nightly extends Freq
    case object Weekly extends Freq
    case object Monthly extends Freq
    val all: List[Freq] = List(Hourly, Daily, Nightly, Weekly, Monthly)
    def apply(name: String) = all find (_.name == name)
  }

  sealed trait Speed {
    def name = toString.toLowerCase
  }
  object Speed {
    case object Bullet extends Speed
    case object SuperBlitz extends Speed
    case object Blitz extends Speed
    case object Classical extends Speed
    val all: List[Speed] = List(Bullet, SuperBlitz, Blitz, Classical)
    def apply(name: String) = all find (_.name == name)
  }

  private[tournament] def durationFor(sched: Schedule): Option[Int] = {
    import Freq._, Speed._
    Some((sched.freq, sched.speed) match {

      case (Hourly, Bullet)              => 40
      case (Hourly, SuperBlitz)          => 50
      case (Hourly, Blitz)               => 50
      case (Hourly, Classical)           => 0 // N/A

      case (Daily | Nightly, Bullet)     => 50
      case (Daily | Nightly, SuperBlitz) => 80
      case (Daily | Nightly, Blitz)      => 80
      case (Daily | Nightly, Classical)  => 120

      case (Weekly, Bullet)              => 60
      case (Weekly, SuperBlitz)          => 100
      case (Weekly, Blitz)               => 100
      case (Weekly, Classical)           => 150

      case (Monthly, Bullet)             => 90
      case (Monthly, SuperBlitz)         => 120
      case (Monthly, Blitz)              => 120
      case (Monthly, Classical)          => 180
    }) filter (0!=)
  }

  private[tournament] def clockFor(sched: Schedule) = sched.speed match {
    case Speed.Bullet     => TournamentClock(60, 0)
    case Speed.SuperBlitz => TournamentClock(3 * 60, 0)
    case Speed.Blitz      => TournamentClock(5 * 60, 0)
    case Speed.Classical  => TournamentClock(10 * 60, 0)
  }
}
