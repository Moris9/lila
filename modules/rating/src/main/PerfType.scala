package lila.rating

import chess.Speed

sealed abstract class PerfType(
  val id: Int,
  val key: Perf.Key,
  val name: String,
  val title: String,
  val iconChar: Char)

object PerfType {

  case object Bullet extends PerfType(1,
    key = "bullet",
    name = Speed.Bullet.name,
    title = Speed.Bullet.title,
    iconChar = 'T')

  case object Blitz extends PerfType(2,
    key = "blitz",
    name = Speed.Blitz.name,
    title = Speed.Blitz.title,
    iconChar = ')')

  case object Classical extends PerfType(3,
    key = "classical",
    name = Speed.Classical.name,
    title = Speed.Classical.title,
    iconChar = '+')

  case object Correspondence extends PerfType(4,
    key = "correspondence",
    name = "Correspondence",
    title = "Correspondence (days per turn)",
    iconChar = ';')

  case object Standard extends PerfType(5,
    key = "standard",
    name = chess.variant.Standard.name,
    title = "Standard rules of chess",
    iconChar = '8')

  case object Chess960 extends PerfType(11,
    key = "chess960",
    name = chess.variant.Chess960.name,
    title = "Chess960 variant",
    iconChar = ''')

  case object KingOfTheHill extends PerfType(12,
    key = "kingOfTheHill",
    name = chess.variant.KingOfTheHill.name,
    title = "King of the Hill variant",
    iconChar = '(')

  case object Antichess extends PerfType(13,
    key = "antichess",
    name = chess.variant.Antichess.name,
    title = "Antichess variant",
    iconChar = '@'
  )

  case object Atomic extends PerfType(14,
    key="atomic",
    name= chess.variant.Atomic.name,
    title = "Atomic variant",
    iconChar = '>'
  )

  case object ThreeCheck extends PerfType(15,
    key = "threeCheck",
    name = chess.variant.ThreeCheck.name,
    title = "Three-check variant",
    iconChar = '.')

  case object Horde extends PerfType(16,
    key = "horde",
    name = chess.variant.Horde.name,
    title = "Horde variant",
    iconChar = '_')

  case object Puzzle extends PerfType(20,
    key = "puzzle",
    name = "Training",
    title = "Training puzzles",
    iconChar = '-')

  case object Opening extends PerfType(21,
    key = "opening",
    name = "Opening",
    title = "Opening trainer",
    iconChar = ']')

  val all: List[PerfType] = List(Bullet, Blitz, Classical, Correspondence, Standard, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, Puzzle, Opening)
  val byKey = all map { p => (p.key, p) } toMap

  val default = Standard

  def apply(key: Perf.Key): Option[PerfType] = byKey get key
  def orDefault(key: Perf.Key): PerfType = apply(key) | default

  def name(key: Perf.Key): Option[String] = apply(key) map (_.name)

  val nonPuzzle: List[PerfType] = List(Bullet, Blitz, Classical, Correspondence, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde)
  val nonGame: List[PerfType] = List(Puzzle, Opening)
  val leaderboardable: List[PerfType] = List(Bullet, Blitz, Classical, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde)
  val variants: List[PerfType] = List(Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde)
}
