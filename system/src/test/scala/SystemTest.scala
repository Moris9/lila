package lila.system

import org.specs2.mutable.Specification
import ornicar.scalalib.test.OrnicarValidationMatchers

import model._
import lila.chess._
import format.Visual

trait SystemTest
    extends Specification
    with OrnicarValidationMatchers
    with Fixtures {

  implicit def stringToBoard(str: String): Board = Visual << str

  implicit def richDbGame(dbGame: DbGame) = new {

    def withoutEvents: DbGame = dbGame.copy(
      players = dbGame.players map (_.copy(evts = ""))
    )

    def afterMove(orig: Pos, dest: Pos): Valid[DbGame] =
      dbGame.toChess.apply(orig, dest) map {
        case (ng, m) ⇒ dbGame.update(ng, m)
      }
  }

  def addNewLines(str: String) = "\n" + str + "\n"
}
