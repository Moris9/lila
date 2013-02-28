package lila.app
package round

import game.DbGame
import lila.common.memo.BooleanExpiryMemo

import scalaz.effects._

final class FinisherLock(timeout: Int) {

  private val internal = new BooleanExpiryMemo(timeout)

  def isLocked(game: DbGame): Boolean = internal get game.id

  def lock(game: DbGame): IO[Unit] = internal put game.id
}
