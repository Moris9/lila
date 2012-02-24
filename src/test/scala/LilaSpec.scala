package lila

import ornicar.scalalib.test.OrnicarValidationMatchers
import org.specs2.mutable.Specification
import org.specs2.matcher.{ Matcher }

import model._
import format.Visual

trait LilaSpec
    extends Specification
    with OrnicarValidationMatchers {

  def bePoss(poss: Pos*): Matcher[Valid[Iterable[Pos]]] = beSuccess.like {
    case p ⇒ p.toList.sorted must_== poss.toList.sorted
  }

  def bePoss(board: Board, visual: String): Matcher[Valid[Iterable[Pos]]] = beSuccess.like {
    case p ⇒ Visual.addNewLines(Visual.>>|(board, Map(p -> 'x'))) must_== visual
  }

}
