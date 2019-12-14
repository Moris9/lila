package lila.team

import lila.memo.Syncache
import scala.concurrent.duration._

final class Cached(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo,
    asyncCache: lila.memo.AsyncCache.Builder
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  val nameCache = new Syncache[String, Option[String]](
    name = "team.name",
    initialCapacity = 4096,
    compute = teamRepo.name,
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(30 minutes),
    logger = logger
  )

  def blockingTeamName(id: Team.ID) = nameCache sync id

  def preloadSet = nameCache preloadSet _

  // ~ 50k entries as of 14/12/19
  private val teamIdsCache = new Syncache[lila.user.User.ID, Team.IdsStr](
    name = "team.ids",
    initialCapacity = 32768,
    compute = u => memberRepo.teamIdsByUser(u).dmap(Team.IdsStr.apply),
    default = _ => Team.IdsStr.empty,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(1 hour),
    logger = logger
  )

  def syncTeamIds                            = teamIdsCache sync _
  def teamIds                                = teamIdsCache async _
  def teamIdsList(userId: lila.user.User.ID) = teamIds(userId).dmap(_.toList)

  def invalidateTeamIds = teamIdsCache invalidate _

  val nbRequests = asyncCache.clearable[lila.user.User.ID, Int](
    name = "team.nbRequests",
    f = userId => teamRepo teamIdsByCreator userId flatMap requestRepo.countByTeams,
    expireAfter = _.ExpireAfterAccess(15 minutes)
  )
}
