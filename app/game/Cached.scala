package lila
package game

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import akka.dispatch.{ Future, Await }
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

import play.api.Play.current
import play.api.libs.concurrent._

import memo.ActorMemo

final class Cached(
    gameRepo: GameRepo,
    nbTtl: Int) {

  import Cached._

  def nbGames: Int = memo(NbGames)
  def nbMates: Int = memo(NbMates)

  private val memo = ActorMemo(loadFromDb, nbTtl, 5.seconds)

  private def loadFromDb(key: Key) = key match {
    case NbGames ⇒ gameRepo.count(_.all).unsafePerformIO
    case NbMates ⇒ gameRepo.count(_.mate).unsafePerformIO
  }
}

object Cached {

  sealed trait Key

  case object NbGames extends Key
  case object NbMates extends Key
}
