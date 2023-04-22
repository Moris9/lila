package lila.tree

import alleycats.Zero
import chess.Centis
import chess.format.pgn.{ Node as ChessNode }
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
    clock: Option[Centis],
    crazyData: Option[Crazyhouse.Data]
    // TODO, add support for variationComments
)

case class NewBranch(
    id: UciCharPair,
    move: Uci.WithSan,
    comp: Boolean, // generated by a computer analysis
    forceVariation: Boolean,
    metas: Metas
)

type NewTree = ChessNode[NewBranch]

object NewTree:
  // default case class constructor not working with type alias?
  def apply(value: NewBranch, child: Option[NewTree], variation: Option[NewTree]) =
    ChessNode(value, child, variation)

  extension [A](xs: List[ChessNode[A]])
    def toVariations: Option[ChessNode[A]] =
      xs.reverse.foldLeft(none[ChessNode[A]])((acc, x) => x.copy(variation = acc).some)

    def toChild: Option[ChessNode[A]] =
      xs.reverse.foldLeft(none[ChessNode[A]])((acc, x) => x.copy(child = acc).some)

  extension [A](xs: List[A])
    def toVariations[B](f: A => ChessNode[B]) =
      xs.reverse.foldLeft(none[ChessNode[B]])((acc, x) => f(x).copy(variation = acc).some)

    def toChild[B](f: A => ChessNode[B]) =
      xs.reverse.foldLeft(none[ChessNode[B]])((acc, x) => f(x).copy(child = acc).some)

case class NewRoot(metas: Metas, tree: NewTree)
