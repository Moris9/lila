package lila
package model

import Pos._
import format.Visual

class RookTest extends LilaSpec {

  "a rook" should {

    val rook = White - Rook

    def moves(pos: Pos): Valid[Set[Pos]] = Board.empty place rook at pos flatMap { b ⇒
      b actorAt pos map (_.moves)
    }

    "move to any position along the same rank or file" in {
      moves(E4) must bePoss(E5, E6, E7, E8, E3, E2, E1, F4, G4, H4, D4, C4, B4, A4)
    }

    "move to any position along the same rank or file, even when at the edges" in {
      moves(H8) must bePoss(H7, H6, H5, H4, H3, H2, H1, G8, F8, E8, D8, C8, B8, A8)
    }

    "not move to positions that are occupied by the same colour" in {
      Visual << """
k B



N R    P

PPPPPPPP
 NBQKBNR
""" movesFrom C4 must bePoss(C3, C5, C6, C7, B4, D4, E4, F4, G4)
    }

    "capture opponent pieces" in {
      Visual << """
k
  b


n R   p

PPPPPPPP
 NBQKBNR
""" movesFrom C4 must bePoss(C3, C5, C6, C7, B4, A4, D4, E4, F4, G4)
    }
  }
}
