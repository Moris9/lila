package lila.game

import scala.concurrent.duration._

import play.api.libs.json._

import lila.common.Scheduler
import lila.db.api._
import lila.hub.actorApi.bookmark.Remove
import tube.gameTube

private[game] final class Maintenance(
    scheduler: Scheduler,
    bookmark: akka.actor.ActorSelection) {

  def remove(ids: List[GameRepo.ID]) {
    $remove[Game]($select byIds ids)
    PgnRepo removeIds ids
    bookmark ! Remove(ids)
  }

  def remove(id: GameRepo.ID) {
    remove(id :: Nil)
  }

  def cleanupUnplayed: Funit =
    $primitive(Query.unplayed, "_id", max = 10000.some)(_.asOpt[String]) addEffect { ids ⇒
      println("[titivate] Remove %d unplayed games" format ids.size)
      scheduler.throttle(1.second)(ids grouped 20 toSeq)(remove)
    } void
}
