package lila.game

import com.github.blemale.scaffeine.Scaffeine
import scala.concurrent.duration._

import lila.user.{ User, UserRepo }

final class BestOpponents(
    userRepo: UserRepo,
    gameRepo: GameRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val limit = 30

  private val userIdsCache = Scaffeine()
    .expireAfterWrite(15 minutes)
    .buildAsyncFuture[User.ID, List[(User.ID, Int)]] { gameRepo.bestOpponents(_, limit) }

  def apply(userId: String): Fu[List[(User, Int)]] =
    userIdsCache get userId flatMap { opponents =>
      userRepo enabledByIds opponents.map(_._1) map {
        _ flatMap { user =>
          opponents find (_._1 == user.id) map { opponent =>
            user -> opponent._2
          }
        } sortBy (-_._2)
      }
    }
}
