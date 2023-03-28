package lila.simul

import chess.{ Centis, Clock, Color }
import chess.Clock.{ LimitSeconds, LimitMinutes }

case class SimulClock(
    config: Clock.Config,
    hostExtraTime: LimitSeconds,
    hostExtraTimePerPlayer: LimitSeconds
):

  def chessClockOf(hostColor: Color) =
    config.toClock.giveTime(
      hostColor,
      Centis.ofSeconds {
        hostExtraTime.atLeast(-config.limitSeconds + 20).value
      }
    )

  def hostExtraMinutes          = LimitMinutes(hostExtraTime.value / 60)
  def hostExtraMinutesPerPlayer = LimitMinutes(hostExtraTimePerPlayer.value / 60)

  def adjustedForPlayers(numberOfPlayers: Int) =
    copy(hostExtraTime = hostExtraTime + numberOfPlayers * hostExtraTimePerPlayer.value)

  def valid =
    if (config.limitSeconds + hostExtraTime == LimitSeconds(0)) config.incrementSeconds >= 10
    else config.limitSeconds + hostExtraTime > 0
