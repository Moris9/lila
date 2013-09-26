package lila.game

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    hub: lila.hub.Env,
    appPath: String,
    isProd: Boolean,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CachedNbTtl = config duration "cached.nb.ttl"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val CaptcherName = config getString "captcher.name"
    val CollectionGame = config getString "collection.game"
    val CollectionPgn = config getString "collection.pgn"
    val JsPathRaw = config getString "js_path.raw"
    val JsPathCompiled = config getString "js_path.compiled"
    val ActorName = config getString "actor.name"
    val FeaturedContinue = config duration "featured.continue"
    val FeaturedDisrupt = config duration "featured.disrupt"
  }
  import settings._

  private[game] lazy val gameColl = db(CollectionGame)

  private[game] lazy val pgnColl = db(CollectionPgn)

  lazy val cached = new Cached(ttl = CachedNbTtl)

  lazy val paginator = new PaginatorBuilder(
    cached = cached,
    maxPerPage = PaginatorMaxPerPage)

  lazy val featured = new Featured(
    lobbySocket = hub.socket.lobby,
    roundActor = hub.actor.round,
    rendererActor = hub.actor.renderer,
    system = system)

  lazy val export = new Export(hub.actor.router).apply _

  lazy val listMenu = ListMenu(cached) _

  lazy val rewind = Rewind

  lazy val gameJs = new GameJs(path = jsPath, useCache = isProd)

  // load captcher actor
  private val captcher = system.actorOf(Props(new Captcher), name = CaptcherName)

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.game.Count ⇒ cached.nbGames pipeTo sender
    }
  }), name = ActorName)

  {
    import scala.concurrent.duration._

    scheduler.effect(0.9 hours, "game: cleanup") {
      maintenance.cleanupUnplayed 
    }

    scheduler.message(10.seconds) {
      captcher -> actorApi.NewCaptcha
    }

    scheduler.message(FeaturedContinue) {
      featured.actor -> Featured.Continue
    }
    scheduler.message(FeaturedDisrupt) {
      featured.actor -> Featured.Disrupt
    }
  }

  def cli = new Cli(computeElos)

  private lazy val computeElos = new ComputeElos(system)

  lazy val maintenance = new Maintenance(scheduler, hub.actor.bookmark)

  private def jsPath =
    "%s/%s".format(appPath, isProd.fold(JsPathCompiled, JsPathRaw))
}

object Env {

  private def app = play.api.Play.current

  lazy val current = "[boot] game" describes new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    appPath = app.path.getCanonicalPath,
    isProd = lila.common.PlayApp.isProd,
    scheduler = lila.common.PlayApp.scheduler
  )
}
