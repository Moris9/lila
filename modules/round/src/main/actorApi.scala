package lila.round
package actorApi

import scala.concurrent.Promise

import chess.format.Uci
import chess.{ MoveMetrics, Color }

import lila.common.{ IpAddress, IsMobile }
import lila.socket.Socket.{ SocketVersion, Sri }
import lila.user.User
import lila.game.Game.{ FullId, PlayerId }

case class EventList(events: List[lila.game.Event])
case class UserTv(userId: User.ID, reload: Fu[Boolean])
case class ByePlayer(playerId: PlayerId)
case class IsGone(color: Color, promise: Promise[Boolean])
case class GetSocketStatus(promise: Promise[SocketStatus])
case class SocketStatus(
    version: SocketVersion,
    whiteOnGame: Boolean,
    whiteIsGone: Boolean,
    blackOnGame: Boolean,
    blackIsGone: Boolean
) {
  def onGame(color: Color) = color.fold(whiteOnGame, blackOnGame)
  def isGone(color: Color) = color.fold(whiteIsGone, blackIsGone)
  def colorsOnGame: Set[Color] = Color.all.filter(onGame).toSet
}
case class RoomCrowd(white: Boolean, black: Boolean)
object RoomIsEmpty
case class SetGame(game: Option[lila.game.Game])
case object GetGame
case class BotConnected(color: Color, v: Boolean)

package round {

  case class HumanPlay(
      playerId: PlayerId,
      uci: Uci,
      blur: Boolean,
      moveMetrics: MoveMetrics = MoveMetrics(),
      promise: Option[Promise[Unit]] = None
  ) {

    val trace = lila.mon.round.move.trace.create
  }

  case class PlayResult(events: Events, fen: String, lastMove: Option[String])

  case object AbortForMaintenance
  case object AbortForce
  case object Threefold
  case object ResignAi
  case class ResignForce(playerId: PlayerId)
  case class DrawForce(playerId: PlayerId)
  case class DrawClaim(playerId: PlayerId)
  case class DrawYes(playerId: PlayerId)
  case class DrawNo(playerId: PlayerId)
  case class TakebackYes(playerId: PlayerId)
  case class TakebackNo(playerId: PlayerId)
  case class Moretime(playerId: PlayerId)
  case object QuietFlag
  case class ClientFlag(color: Color, fromPlayerId: Option[PlayerId])
  case object Abandon
  case class ForecastPlay(lastMove: chess.Move)
  case class Cheat(color: Color)
  case class HoldAlert(playerId: PlayerId, mean: Int, sd: Int, ip: IpAddress)
  case class GoBerserk(color: Color)
  case object NoStart
  case object TooManyPlies
}

private[round] case object GetNbRounds
private[round] case object NotifyCrowd
