package lila.lobby

import scala.util.Random.nextBoolean

sealed abstract class Color(val name: String) {

  def resolve: chess.Color

  def compatibleWith(c: Color) = (c == this).fold(c == Color.Random, true)
}

object Color {

  object White extends Color("white") {

    val resolve = chess.White
  }

  object Black extends Color("black") {

    val resolve = chess.Black
  }

  object Random extends Color("random") {

    def resolve = nextBoolean.fold(White, Black).resolve
  }

  def apply(name: String): Option[Color] = all find (_.name == name)

  def orDefault(name: String) = apply(name) | default

  val all = List(White, Black, Random)

  def random = all(scala.util.Random.nextInt(all.size))

  val names = all map (_.name)

  val choices = names zip names

  val default = Random
}
