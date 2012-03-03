package lila.chess

import format.PgnDump

case class Game(
    board: Board = Board(),
    player: Color = White,
    pgnMoves: String = "",
    clock: Option[Clock] = None,
    deads: List[(Pos, Piece)] = Nil) {

  def playMove(
    orig: Pos,
    dest: Pos,
    promotion: PromotableRole = Queen): Valid[Game] = for {
    move ← situation.move(orig, dest, promotion)
  } yield {
    val newGame = copy(
      board = move.afterWithPositionHashesUpdated,
      player = !player,
      deads = (for {
        cpos ← move.capture
        cpiece ← board(cpos)
      } yield (cpos, cpiece) :: deads) getOrElse deads
    )
    val pgnMove = PgnDump.move(situation, move, newGame.situation)
    newGame.copy(pgnMoves = (pgnMoves + " " + pgnMove).trim)
  }

  lazy val situation = Situation(board, player)

  def pgnMovesList = pgnMoves.split(' ').toList
}
