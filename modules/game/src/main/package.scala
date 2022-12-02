package lila.game

import alleycats.Zero
import cats.kernel.Monoid
import chess.PositionHash

export lila.Lila.{ *, given }

type PgnMoves    = Vector[String]
type RatingDiffs = chess.Color.Map[IntRatingDiff]

given Zero[PositionHash] = Zero(Monoid[PositionHash].empty)

private val logger = lila.log("game")
