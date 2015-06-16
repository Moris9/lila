package lila.tv

import com.typesafe.config.Config

import lila.common.PimpedConfig._

import scala.collection.JavaConversions._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    system: akka.actor.ActorSystem,
    scheduler: lila.common.Scheduler,
    isProd: Boolean) {

  private val FeaturedSelect = config duration "featured.select"
  private val StreamingSearch = config duration "streaming.search"
  private val UstreamApiKey = config getString "streaming.ustream_api_key"
  private val CollectionWhitelist = config getString "streaming.collection.whitelist"

  lazy val tv = new Tv(
    rendererActor = hub.actor.renderer,
    system = system)

  private lazy val streaming = new Streaming(
    system = system,
    renderer = hub.actor.renderer,
    ustreamApiKey = UstreamApiKey,
    whitelist = whitelist)

  private lazy val whitelist = new Whitelist(db(CollectionWhitelist))

  def streamsOnAir = streaming.onAir

  {
    import scala.concurrent.duration._

    // scheduler.message(isProd.fold(FeaturedContinue, 10.seconds)) {
    scheduler.message(FeaturedSelect) {
      tv.actor -> TvActor.Select
    }

    scheduler.once(20.seconds) {
      streaming.actor ! Streaming.Search
      scheduler.message(StreamingSearch) {
        streaming.actor -> Streaming.Search
      }
    }
  }
}

object Env {

  lazy val current = "[boot] tv" describes new Env(
    config = lila.common.PlayApp loadConfig "tv",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    isProd = lila.common.PlayApp.isProd)
}

