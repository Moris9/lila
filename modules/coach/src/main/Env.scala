package lila.coach

import scala.concurrent.duration._
import com.typesafe.config.Config
import akka.actor._

final class Env(
    config: Config,
    notifyApi: lila.notify.NotifyApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: ActorSystem,
    db: lila.db.Env
) {

  private val CollectionCoach = config getString "collection.coach"
  private val CollectionReview = config getString "collection.review"
  private val CollectionImage = config getString "collection.image"

  private lazy val coachColl = db(CollectionCoach)
  private lazy val reviewColl = db(CollectionReview)
  private lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new lila.db.Photographer(imageColl, "coach")

  lazy val api = new CoachApi(
    coachColl = coachColl,
    reviewColl = reviewColl,
    photographer = photographer,
    asyncCache = asyncCache,
    notifyApi = notifyApi
  )

  lazy val pager = new CoachPager(api)

  system.lilaBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
          system.scheduler.scheduleOnce(5 minutes) { api.reviews.deleteAllBy(userId) }
      }
    })),
    'adjustCheater
  )

  def cli = new lila.common.Cli {
    def process = {
      case "coach" :: "enable" :: username :: Nil => api.toggleApproved(username, true)
      case "coach" :: "disable" :: username :: Nil => api.toggleApproved(username, false)
    }
  }
}

object Env {

  lazy val current: Env = "coach" boot new Env(
    config = lila.common.PlayApp loadConfig "coach",
    notifyApi = lila.notify.Env.current.api,
    asyncCache = lila.memo.Env.current.asyncCache,
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current
  )
}
