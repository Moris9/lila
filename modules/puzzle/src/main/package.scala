package lila

import lila.rating.Glicko

package object puzzle extends PackageObject:

  private[puzzle] def logger = lila.log("puzzle")

package puzzle:

  case class PuzzleResult(win: Boolean) extends AnyVal:
    def loss = !win
