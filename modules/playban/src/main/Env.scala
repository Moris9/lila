package lila.playban

import com.typesafe.config.Config

final class Env(
    config: Config,
    isRematch: String => Boolean,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionPlayban = config getString "collection.playban"
  }
  import settings._

  lazy val api = new PlaybanApi(coll = db(CollectionPlayban), isRematch = isRematch)
}

object Env {

  lazy val current: Env = "playban" boot new Env(
    config = lila.common.PlayApp loadConfig "playban",
    isRematch = lila.game.Env.current.cached.isRematch.get _,
    db = lila.db.Env.current)
}
