package lila
package setup

import core.Settings
import game.{ DbGame, GameRepo }
import lobby.Fisherman
import round.Messenger
import ai.Ai
import user.{ User, UserRepo }

import com.mongodb.casbah.MongoCollection
import scalaz.effects._
import com.mongodb.DBRef

final class SetupEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo,
    fisherman: Fisherman,
    userRepo: UserRepo,
    timelinePush: DbGame ⇒ IO[Unit],
    roundMessenger: Messenger,
    ai: () ⇒ Ai,
    dbRef: User ⇒ DBRef) {

  import settings._

  lazy val configRepo = new UserConfigRepo(mongodb(MongoCollectionConfig))

  lazy val formFactory = new FormFactory(
    configRepo = configRepo)

  lazy val processor = new Processor(
    configRepo = configRepo,
    friendConfigMemo = friendConfigMemo,
    gameRepo = gameRepo,
    fisherman = fisherman,
    timelinePush = timelinePush,
    ai = ai,
    dbRef = dbRef)

  lazy val friendConfigMemo = new FriendConfigMemo(
    ttl = SetupFriendConfigMemoTtl)

  lazy val rematcher = new Rematcher(
    gameRepo = gameRepo,
    userRepo = userRepo,
    messenger = roundMessenger,
    timelinePush = timelinePush)

  lazy val joiner = new Joiner(
    gameRepo = gameRepo,
    messenger = roundMessenger,
    timelinePush = timelinePush,
    dbRef = dbRef)
}
