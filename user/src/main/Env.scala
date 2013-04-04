package lila.user

import lila.common.PimpedConfig._

import chess.EloCalculator
import com.typesafe.config.Config

final class Env(config: Config, db: lila.db.Env) {

  private val settings = new {
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val EloUpdaterFloor = config getInt "elo_updater.floor"
    val CachedNbTtl = config duration "cached.nb.ttl"
    val OnlineTtl = config duration "online.ttl"
    val CollectionUser = config getString "collection.user"
    val CollectionHistory = config getString "collection.history"
    val CollectionConfig = config getString "collection.config"
  }
  import settings._

  lazy val historyColl = db(CollectionHistory)

  lazy val userColl = db(CollectionUser)

  lazy val paginator = new PaginatorBuilder(
    countUsers = cached.countEnabled,
    maxPerPage = PaginatorMaxPerPage)

  lazy val eloUpdater = new EloUpdater(floor = EloUpdaterFloor)

  lazy val usernameMemo = new UsernameMemo(ttl = OnlineTtl)

  private lazy val cached = new Cached(ttl = CachedNbTtl)

  def usernameOption(id: String): Fu[Option[String]] = cached username id

  def usernameOrAnonymous(id: String): Fu[String] = cached usernameOrAnonymous id

  def cli = new lila.common.Cli {
    import play.api.libs.concurrent.Execution.Implicits._
    def process = {
      case "user" :: "average" :: "elo" :: Nil ⇒
        print("ooooooooooooooooooooo"); UserRepo.averageElo map { elo ⇒ "Average elo is %f" format elo }
    }
  }
}

object Env {

  lazy val current: Env = "[user] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "user",
    db = lila.db.Env.current)
}
