package lila.game

import chess.Color

case class Pov(game: Game, color: Color) {

  def player = game player color

  def playerId = player.id

  def fullId = game fullIdOf color

  def gameId = game.id

  def opponent = game player !color

  def unary_! = Pov(game, !color)

  def isPlayerFullId(fullId: Option[String]): Boolean =
    fullId some { game.isPlayerFullId(player, _) } none false

  def ref = PovRef(game.id, color)

  def withGame(g: Game) = Pov(g, color)

  override def toString = ref.toString
}

object Pov {

  def apply(game: Game): List[Pov] = game.players.map { apply(game, _) }

  def apply(game: Game, player: Player) = new Pov(game, player.color)

  def apply(game: Game, playerId: String): Option[Pov] =
    game player playerId map { p ⇒ new Pov(game, p.color) }
}

case class PovRef(gameId: String, color: Color) {

  def unary_! = PovRef(gameId, !color)
}

case class PlayerRef(gameId: String, playerId: String)

object PlayerRef {

  def apply(fullId: String): PlayerRef = PlayerRef(Game takeGameId fullId, Game takePlayerId fullId)
}
