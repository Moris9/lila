package lila
package setup

import scala.util.Random.nextBoolean

sealed abstract class Color(val name: String) {

  val resolve: chess.Color
}

object White extends Color("white") {

  val resolve = chess.White
}

object Black extends Color("black") {

  val resolve = chess.Black
}

object Random extends Color("random") {

  val resolve = nextBoolean.fold(White, Black).resolve
}

object Color {

  def apply(name: String): Option[Color] = all find (_.name == name)

  val all = List(White, Black, Random)

  val names = all map (_.name)

  val choices = names zip names

  val default = White
}
