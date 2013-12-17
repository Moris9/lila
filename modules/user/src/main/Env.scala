package lila.user

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.memo.ExpireSetMemo

final class Env(
    config: Config,
    db: lila.db.Env,
    scheduler: lila.common.Scheduler,
    system: ActorSystem) {

  private val settings = new {
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val CachedNbTtl = config duration "cached.nb.ttl"
    val CachedRatingChartTtl = config duration "cached.rating_chart.ttl"
    val OnlineTtl = config duration "online.ttl"
    val RankingTtl = config duration "ranking.ttl"
    val CollectionUser = config getString "collection.user"
    val CollectionHistory = config getString "collection.history"
  }
  import settings._

  lazy val historyColl = db(CollectionHistory)

  lazy val userColl = db(CollectionUser)

  lazy val paginator = new PaginatorBuilder(
    countUsers = cached.countEnabled,
    maxPerPage = PaginatorMaxPerPage)

  lazy val onlineUserIdMemo = new ExpireSetMemo(ttl = OnlineTtl)

  lazy val ranking = new Ranking(ttl = RankingTtl)

  def ratingChart = cached.ratingChart.apply _

  val forms = DataForm

  def usernameOption(id: String): Fu[Option[String]] = cached username id

  def usernameOrAnonymous(id: String): Fu[String] = cached usernameOrAnonymous id

  def isOnline(userId: String) = onlineUserIdMemo get userId

  def countEnabled = cached.countEnabled

  def cli = new lila.common.Cli {
    import tube.userTube
    def process = {
      case "user" :: "average" :: "rating" :: Nil ⇒
        UserRepo.averageRating map { rating ⇒ "Average rating is %f" format rating }
      case "user" :: "typecheck" :: Nil ⇒ lila.db.Typecheck.apply[User]
    }
  }

  private val bus = system.lilaBus

  bus.subscribe(system.actorOf(
    Props(new Actor {
      def receive = {
        case User.Active(user, lang) ⇒ {
          if (!user.seenRecently) UserRepo setSeenAt user.id
          if (user.lang != lang.some) UserRepo.setLang(user.id, lang)
          onlineUserIdMemo put user.id
        }
      }
    }), name = "user-active"), 'userActive)

  {
    import scala.concurrent.duration._
    import lila.hub.actorApi.WithUserIds

    scheduler.effect(3 seconds, "refresh online user ids") {
      bus.publish(WithUserIds(onlineUserIdMemo.putAll), 'users)
    }
  }

  lazy val cached = new Cached(
    nbTtl = CachedNbTtl,
    ratingChartTtl = CachedRatingChartTtl,
    onlineUserIdMemo = onlineUserIdMemo)
}

object Env {

  lazy val current: Env = "[boot] user" describes new Env(
    config = lila.common.PlayApp loadConfig "user",
    db = lila.db.Env.current,
    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system)
}
