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

  // case object Swiss extends System(id = 2) {
  //   val pairingSystem = swiss.SwissSystem
  //   val scoringSystem = swiss.SwissSystem
  //   val berserkable = false
  // }

  val default = Arena

  // val all = List(Arena, Swiss)
  val all = List(Arena)

  val byId = all map { s => (s.id -> s) } toMap

  def apply(id: Int): Option[System] = byId get id
  def orDefault(id: Int): System = apply(id) getOrElse default
}

trait PairingSystem {
  def createPairings(
    tournament: Tournament,
    users: AllUserIds): Fu[(Pairings, Events)]
}

trait Score {
  val value: Int
}

trait ScoreSheet {
  def scores: List[Score]
  def total: Int
}

trait ScoringSystem {
  type Sheet <: ScoreSheet

  def emptySheet: Sheet

  def sheet(tournament: Tournament, userId: String): Fu[Sheet]
}
