package lila.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject

import lila.db.api.$count
import lila.memo.{ AsyncCache, ExpireSetMemo }
import tube.userTube

final class Cached(
    nbTtl: Duration,
    ratingChartTtl: Duration,
    onlineUserIdMemo: ExpireSetMemo) {

  val username = AsyncCache(UserRepo.usernameById, maxCapacity = 50000)

  def usernameOrAnonymous(id: String): Fu[String] =
    username(id) map (_ | User.anonymous)

  def usernameOrAnonymous(id: Option[String]): Fu[String] =
    id.fold(fuccess(User.anonymous))(usernameOrAnonymous)

  val count = AsyncCache((o: JsObject) ⇒ $count(o), timeToLive = nbTtl)

  def countEnabled: Fu[Int] = count(UserRepo.enabledSelect)

  val ratingChart = AsyncCache(RatingChart.apply,
    maxCapacity = 5000,
    timeToLive = ratingChartTtl)

  private def oneDayAgo = DateTime.now minusDays 1
  private def oneWeekAgo = DateTime.now minusWeeks 1
  private def oneMonthAgo = DateTime.now minusMonths 1
  val topProgressDay = AsyncCache(
    (nb: Int) ⇒ UserRepo.topProgressSince(oneDayAgo, nb),
    timeToLive = 14 minutes)
  val topProgressWeek = AsyncCache(
    (nb: Int) ⇒ UserRepo.topProgressSince(oneWeekAgo, nb),
    timeToLive = 29 minutes)
  val topProgressMonth = AsyncCache(
    (nb: Int) ⇒ UserRepo.topProgressSince(oneMonthAgo, nb),
    timeToLive = 27 minutes)
  val topRatingDay = AsyncCache(
    (nb: Int) ⇒ UserRepo.topRatingSince(oneDayAgo, nb),
    timeToLive = 13 minutes)
  val topRatingWeek = AsyncCache(
    (nb: Int) ⇒ UserRepo.topRatingSince(oneWeekAgo, nb),
    timeToLive = 28 minutes)
  val topRating = AsyncCache(UserRepo.topRating, timeToLive = 30 minutes)
  val topBullet = AsyncCache(UserRepo.topBullet, timeToLive = 31 minutes)
  val topBlitz = AsyncCache(UserRepo.topBlitz, timeToLive = 32 minutes)
  val topSlow = AsyncCache(UserRepo.topSlow, timeToLive = 33 minutes)
  val topNbGame = AsyncCache(UserRepo.topNbGame, timeToLive = 34 minutes)

  val topOnline = AsyncCache(
    (nb: Int) ⇒ UserRepo.byIdsSortRating(onlineUserIdMemo.keys, nb),
    timeToLive = 2 seconds)
}
