package lila.tree

import alleycats.Zero
import chess.Centis
import chess.format.pgn
import chess.format.pgn.{ Glyph, Glyphs }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.opening.Opening
import chess.{ Ply, Square, Check }
import chess.variant.{ Variant, Crazyhouse }
import play.api.libs.json.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.Json.{ *, given }

import Node.{ Comments, Comment, Gamebook, Shapes }

case class Metas(
    ply: Ply,
    fen: Fen.Epd,
    check: Check,
    // None when not computed yet
    dests: Option[Map[Square, List[Square]]],
    drops: Option[List[Square]],
    eval: Option[Eval],
    shapes: Node.Shapes,
    comments: Node.Comments,
    gamebook: Option[Node.Gamebook],
    glyphs: Glyphs,
    opening: Option[Opening],
    comp: Boolean, // generated by a computer analysis
    clock: Option[Centis],
    crazyData: Option[Crazyhouse.Data]
    // TODO, add support for variationComments
)

case class NewBranch(id: UciCharPair, move: Uci.WithSan, metas: Metas, forceVariation: Boolean = false)

type NewTree = pgn.Node[NewBranch]

object NewTree:
  // default case class constructor not working with type alias?
  def make(value: NewBranch, child: Option[NewTree], variations: List[NewTree]) =
    pgn.Node[NewBranch](value, child, variations)

case class NewRoot(metas: Metas, tree: NewTree)
