package lila.challenge

import akka.actor.ActorSelection
import scala.concurrent.duration._

import lila.hub.actorApi.SendTo
import lila.memo.{ MixedCache, AsyncCache }
import lila.user.{ User, UserRepo }

final class ChallengeApi(
    repo: ChallengeRepo,
    jsonView: JsonView,
    userRegister: ActorSelection) {

  import Challenge._

  def allFor(userId: User.ID): Fu[AllChallenges] =
    createdByDestId(userId) zip createdByChallengerId(userId) map (AllChallenges.apply _).tupled

  def create(c: Challenge): Funit = (repo insert c) >> {
    c.destUser ?? notify
    c.challengerUser ?? notify
    funit
  }

  def byId = repo byId _

  val countInFor = AsyncCache(repo.countByDestId, maxCapacity = 20000)

  def createdByChallengerId = repo createdByChallengerId _

  def createdByDestId = repo createdByDestId _

  def createdByDestIds = repo createdByDestIds _

  def accept(c: Challenge) = (repo accept c) >> {
    funit
  }

  def cancel(c: Challenge) = (repo cancel c) >> {
    funit
  }

  def decline(c: Challenge) = (repo decline c) >> {
    funit
  }

  private def notify(user: Registered) {
    allFor(user.id) foreach { all =>
      userRegister ! SendTo(user.id,
        lila.socket.Socket.makeMessage("challenges", jsonView(all)))
    }
  }
}
