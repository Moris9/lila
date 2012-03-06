package lila.system
package model

import lila.chess._
import Pos.{ posAt, piotr }
import Role.forsyth

case class DbGame(
    id: String,
    whitePlayer: DbPlayer,
    blackPlayer: DbPlayer,
    pgn: String,
    status: Status.Value,
    turns: Int,
    clock: Option[Clock],
    lastMove: Option[String],
    positionHashes: String = "",
    castles: String = "KQkq",
    isRated: Boolean = false,
    variant: Variant = Standard) {

  val players = List(whitePlayer, blackPlayer)

  val playersByColor: Map[Color, DbPlayer] = Map(
    White -> whitePlayer,
    Black -> blackPlayer
  )

  def player(color: Color): DbPlayer = color match {
    case White ⇒ whitePlayer
    case Black ⇒ blackPlayer
  }

  def playerById(id: String): Option[DbPlayer] = players find (_.id == id)

  def player: DbPlayer = player(if (0 == turns % 2) White else Black)

  def fullIdOf(player: DbPlayer): Option[String] =
    (players contains player) option id + player.id

  def fullIdOf(color: Color): String = id + player(color).id

  def toChess: Game = {

    def posPiece(posCode: Char, roleCode: Char, color: Color): Option[(Pos, Piece)] = for {
      pos ← piotr(posCode)
      role ← forsyth(roleCode)
    } yield (pos, Piece(color, role))

    val (pieces, deads) = {
      for {
        player ← players
        color = player.color
        piece ← player.ps.split(' ')
      } yield (color, piece(0), piece(1))
    }.foldLeft((Map[Pos, Piece](), List[(Pos, Piece)]())) {
      case ((ps, ds), (color, pos, role)) ⇒ {
        if (role.isUpper) posPiece(pos, role.toLower, color) map { p ⇒ (ps, p :: ds) }
        else posPiece(pos, role, color) map { p ⇒ (ps + p, ds) }
      } getOrElse (ps, ds)
      case (acc, _) ⇒ acc
    }

    Game(
      board = Board(pieces, toChessHistory),
      player = if (0 == turns % 2) White else Black,
      pgnMoves = pgn,
      clock = clock,
      deads = deads,
      turns = turns
    )
  }

  private def toChessHistory = History(
    lastMove = lastMove flatMap {
      case MoveString(a, b) ⇒ for (o ← posAt(a); d ← posAt(b)) yield (o, d)
      case _                ⇒ None
    },
    whiteCastleKingSide = castles contains 'K',
    whiteCastleQueenSide = castles contains 'Q',
    blackCastleKingSide = castles contains 'k',
    blackCastleQueenSide = castles contains 'q',
    positionHashes = positionHashes grouped History.hashSize toList
  )

  def update(game: Game, move: Move): DbGame = {
    val allPieces = (game.board.pieces map {
      case (pos, piece) ⇒ (pos, piece, false)
    }) ++ (game.deads map {
      case (pos, piece) ⇒ (pos, piece, true)
    })
    val (history, situation) = (game.board.history, game.situation)
    val events = (Event fromMove move) ::: (Event fromSituation game.situation)

    def updatePlayer(player: DbPlayer) = player.copy(
      ps = player encodePieces allPieces,
      evts = player.newEvts(events :+ Event.possibleMoves(game.situation, player.color)))

    copy(
      pgn = game.pgnMoves,
      whitePlayer = updatePlayer(whitePlayer),
      blackPlayer = updatePlayer(blackPlayer),
      turns = game.turns,
      positionHashes = history.positionHashes mkString,
      castles = history.castleNotation,
      status =
        if (situation.checkMate) Status.mate
        else if (situation.staleMate) Status.stalemate
        else if (situation.autoDraw) Status.draw
        else status,
      clock = game.clock
    )
  }

  def playable = status < Status.aborted

  def mapPlayers(f: DbPlayer => DbPlayer) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer)
  )
}

object DbGame {

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
}
