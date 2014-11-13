package lila.team

import lila.memo.{ MixedCache, AsyncCache }
import scala.concurrent.duration._

private[team] final class Cached {

  private val nameCache = MixedCache[String, Option[String]](TeamRepo.name,
    timeToLive = 1 hour,
    default = _ => none)

  def name(id: String) = nameCache get id

  private[team] val teamIdsCache = MixedCache[String, List[String]](MemberRepo.teamIdsByUser,
    timeToLive = 30 minutes,
    default = _ => Nil)

  def teamIds(userId: String) = teamIdsCache get userId

  val nbRequests = AsyncCache(
    (userId: String) => TeamRepo teamIdsByCreator userId flatMap RequestRepo.countByTeams,
    maxCapacity = 1024)
}
