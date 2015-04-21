package lila.tournament

sealed abstract class System(val id: Int) {
  val pairingSystem: PairingSystem
  val scoringSystem: ScoringSystem
  val berserkable: Boolean
}

object System {
  case object Arena extends System(id = 1) {
    val pairingSystem = arena.PairingSystem
    val scoringSystem = arena.ScoringSystem
    val berserkable = true
  }

  case object Swiss extends System(id = 2) {
    val pairingSystem = swiss.SwissSystem
    val scoringSystem = swiss.SwissSystem
    val berserkable = false
  }

  val default = Arena

  val all = List(Arena, Swiss)

  val byId = all map { s => (s.id -> s) } toMap

  def apply(id: Int): Option[System] = byId get id
  def orDefault(id: Int): System = apply(id) getOrElse default
}

trait PairingSystem {
  def createPairings(tournament: Tournament, users: AllUserIds): Fu[(Pairings,Events)]
}

trait Score {
  val value: Int
}

trait ScoreSheet {
  def scores: List[Score]
  def total:  Int
}

trait ScoringSystem {
  type Sheet <: ScoreSheet

  // This should rank players by score, and rank all withdrawn players after active ones.
  def rank(tournament: Tournament, players: Players): RankedPlayers

  // You must override either this one or scoreSheet!
  def scoreSheets(tournament: Tournament): Map[String,Sheet] = {
    tournament.players.map { p =>
      (p.id -> scoreSheet(tournament, p.id))
    } toMap
  }

  // You must override either this one or scoreSheets!
  def scoreSheet(tournament: Tournament, player: String): Sheet = scoreSheets(tournament)(player)
}
