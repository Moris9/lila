package lila.tournament

import reactivemongo.api.bson.Macros
import scala.concurrent.duration._

import chess.Color
import lila.db.dsl._

final class TournamentStatsApi(
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    mongoCache: lila.memo.MongoCache.Builder
) {

  def apply(tournament: Tournament): Fu[Option[TournamentStats]] =
    tournament.isFinished ?? cache(tournament.id).map(some)

  implicit private val statsBSONHandler = Macros.handler[TournamentStats]

  private val cache = mongoCache[String, TournamentStats](
    prefix = "tournament:stats",
    keyToString = identity,
    f = fetch,
    timeToLive = 10 minutes,
    timeToLiveMongo = 90.days.some
  )

  private def fetch(tournamentId: Tournament.ID): Fu[TournamentStats] =
    for {
      rating   <- playerRepo.averageRating(tournamentId)
      rawStats <- pairingRepo.rawStats(tournamentId)
    } yield TournamentStats.readAggregation(rating)(rawStats)
}

case class TournamentStats(
    games: Int,
    moves: Int,
    whiteWins: Int,
    blackWins: Int,
    draws: Int,
    berserks: Int,
    averageRating: Int
)

private object TournamentStats {

  private case class ColorStats(games: Int, moves: Int, b1: Int, b2: Int) {
    def berserks = b1 + b2
  }

  def readAggregation(rating: Int)(docs: List[Bdoc]): TournamentStats = {
    val colorStats: Map[Option[Color], ColorStats] = docs.view.map { doc =>
      doc.getAsOpt[Boolean]("_id").map(Color.apply) ->
        ColorStats(
          ~doc.int("games"),
          ~doc.int("moves"),
          ~doc.int("b1"),
          ~doc.int("b2")
        )
    }.toMap
    TournamentStats(
      games = colorStats.foldLeft(0)(_ + _._2.games),
      moves = colorStats.foldLeft(0)(_ + _._2.moves),
      whiteWins = colorStats.get(Color.White.some).??(_.games),
      blackWins = colorStats.get(Color.Black.some).??(_.games),
      draws = colorStats.get(none).??(_.games),
      berserks = colorStats.foldLeft(0)(_ + _._2.berserks),
      averageRating = rating
    )
  }
}
