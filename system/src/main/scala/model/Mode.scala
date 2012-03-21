package lila.system
package model

sealed abstract class Mode(val id: Int)

case object Casual extends Mode(0)
case object Rated extends Mode(1)

object Mode {

  val all = List(Casual, Rated)

  val byId = all map { v ⇒ (v.id, v) } toMap

  def apply(id: Int): Option[Mode] = byId get id
}
