package lila
package game

import chess._
import user.User

import com.mongodb.DBRef

case class DbPlayer(
    id: String,
    color: Color,
    ps: String,
    aiLevel: Option[Int],
    isWinner: Option[Boolean],
    elo: Option[Int],
    isOfferingDraw: Boolean,
    lastDrawOffer: Option[Int],
    isProposingTakeback: Boolean,
    user: Option[DBRef],
    moveTimes: String,
    blurs: Int) {

  def encodePieces(allPieces: Iterable[(Pos, Piece, Boolean)]): String =
    allPieces withFilter (_._2.color == color) map {
      case (pos, piece, dead) ⇒ pos.piotr.toString + {
        if (dead) piece.role.forsyth.toUpper
        else piece.role.forsyth
      }
    } mkString " "

  def withUser(user: User)(dbRef: User => DBRef): DbPlayer = copy(
    user = dbRef(user).some,
    elo = user.elo.some)

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def userId: Option[String] = user map (_.getId.toString)

  def wins = isWinner getOrElse false

  def finish(winner: Boolean) = copy(
    isWinner = if (winner) Some(true) else None
  )

  def offerDraw(turn: Int) = copy(
    isOfferingDraw = true,
    lastDrawOffer = Some(turn)
  )

  def removeDrawOffer = copy(isOfferingDraw = false)

  def proposeTakeback = copy(isProposingTakeback = true)

  def removeTakebackProposition = copy(isProposingTakeback = false)
}

object DbPlayer {

  def apply(
    color: Color,
    aiLevel: Option[Int]): DbPlayer = DbPlayer(
    id = IdGenerator.player,
    color = color,
    ps = "",
    aiLevel = aiLevel,
    isWinner = None,
    elo = None,
    isOfferingDraw = false,
    lastDrawOffer = None,
    isProposingTakeback = false,
    user = None,
    moveTimes = "",
    blurs = 0)
}
