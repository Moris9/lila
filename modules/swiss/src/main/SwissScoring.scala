package lila.swiss

import reactivemongo.api.bson._

import lila.db.dsl._

final private class SwissScoring(
    colls: SwissColls
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def recompute(swiss: Swiss): Fu[SwissScoring.Result] = {
    for {
      (prevPlayers, pairings) <- fetchPlayers(swiss) zip fetchPairings(swiss)
      pairingMap = SwissPairing.toMap(pairings)
      sheets     = SwissSheet.many(swiss, prevPlayers, pairingMap)
      withPoints = (prevPlayers zip sheets).map {
        case (player, sheet) => player.copy(points = sheet.points)
      }
      playerMap = SwissPlayer.toMap(withPoints)
      players = withPoints.map { p =>
        val playerPairings = (~pairingMap.get(p.number)).values
        val (tieBreak, perfSum) = playerPairings.foldLeft(0f -> 0f) {
          case ((tieBreak, perfSum), pairing) =>
            val opponent       = playerMap.get(pairing opponentOf p.number)
            val opponentPoints = opponent.??(_.points.value)
            val result         = pairing.resultFor(p.number)
            val newTieBreak    = tieBreak + result.fold(opponentPoints / 2) { _ ?? opponentPoints }
            val newPerf = perfSum + opponent.??(_.rating) + result.?? { win =>
              if (win) 500 else -500
            }
            newTieBreak -> newPerf
        }
        p.copy(
            tieBreak = Swiss.TieBreak(tieBreak),
            performance = playerPairings.nonEmpty option Swiss.Performance(perfSum / playerPairings.size)
          )
          .recomputeScore
      }
      _ <- SwissPlayer.fields { f =>
        prevPlayers
          .zip(players)
          .filter {
            case (a, b) => a != b
          }
          .map {
            case (prev, player) =>
              colls.player.update
                .one(
                  $id(player.id),
                  $set(
                    f.points      -> player.points,
                    f.tieBreak    -> player.tieBreak,
                    f.performance -> player.performance,
                    f.score       -> player.score
                  )
                )
                .void
          }
          .sequenceFu
          .void
      }
    } yield SwissScoring.Result(
      swiss,
      players.zip(sheets).sortBy(-_._1.score.value),
      SwissPlayer toMap players,
      pairingMap
    )
  }.monSuccess(_.swiss.scoringRecompute)

  private def fetchPlayers(swiss: Swiss) =
    SwissPlayer.fields { f =>
      colls.player.ext
        .find($doc(f.swissId -> swiss.id))
        .sort($sort asc f.number)
        .list[SwissPlayer]()
    }

  private def fetchPairings(swiss: Swiss) =
    !swiss.isCreated ?? SwissPairing.fields { f =>
      colls.pairing.ext
        .find($doc(f.swissId -> swiss.id))
        .list[SwissPairing]()
    }
}

private object SwissScoring {

  case class Result(
      swiss: Swiss,
      leaderboard: List[(SwissPlayer, SwissSheet)],
      playerMap: SwissPlayer.PlayerMap,
      pairings: SwissPairing.PairingMap
  )
}
