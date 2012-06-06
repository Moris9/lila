package lila
package star

import com.mongodb.casbah.MongoCollection

import game.GameRepo
import user.UserRepo
import core.Settings

final class StarEnv(
    settings: Settings,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val starRepo = new StarRepo(mongodb(MongoCollectionStar))

  lazy val api = new StarApi(
    starRepo = starRepo,
    gameRepo = gameRepo,
    userRepo = userRepo)
}
