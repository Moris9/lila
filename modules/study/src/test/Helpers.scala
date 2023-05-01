package lila.study

import monocle.syntax.all.*
import cats.syntax.all.*
import chess.{ Centis, ErrorStr }
import chess.format.pgn.{
  Dumper,
  Glyphs,
  ParsedPgn,
  San,
  Tags,
  PgnStr,
  PgnNodeData,
  Comment as ChessComment,
  Node as PgnNode
}
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.MoveOrDrop.*

import lila.importer.{ ImportData, Preprocessed }
import lila.tree.Node.{ Comment, Comments, Shapes }

import cats.data.Validated
import scala.language.implicitConversions

import lila.tree.{ Branch, Branches, Root, Metas, NewTree, NewBranch, NewRoot, Node }
import chess.Variation

object Helpers:
  import lila.tree.NewTree.*

  // Convertor
  object NewBranchC:
    def fromBranch(branch: Branch) =
      NewBranch(
        branch.id,
        UciPath.root,
        branch.move,
        branch.comp,
        branch.forceVariation,
        MetasC.fromNode(branch)
      )

  extension (newBranch: NewBranch)
    def toBranch(children: Option[NewTree]): Branch = Branch(
      newBranch.id,
      newBranch.metas.ply,
      newBranch.move,
      newBranch.metas.fen,
      newBranch.metas.check,
      newBranch.metas.dests,
      newBranch.metas.drops,
      newBranch.metas.eval,
      newBranch.metas.shapes,
      newBranch.metas.comments,
      newBranch.metas.gamebook,
      newBranch.metas.glyphs,
      children.fold(Branches.empty)(_.toBranches),
      newBranch.metas.opening,
      newBranch.comp,
      newBranch.metas.clock,
      newBranch.metas.crazyData,
      newBranch.forceVariation
    )
  // extension (newBranch: NewBranch)

  extension (newTree: NewTree)
    def toBranch: Branch = newTree.value.toBranch(newTree.child)
    def toBranches: Branches =
      val variations = newTree.variations.map(_.toBranch)
      Branches(newTree.value.toBranch(newTree.child) :: variations)

  extension (newRoot: NewRoot)
    def toRoot =
      Root(
        newRoot.metas.ply,
        newRoot.metas.fen,
        newRoot.metas.check,
        newRoot.metas.dests,
        newRoot.metas.drops,
        newRoot.metas.eval,
        newRoot.metas.shapes,
        newRoot.metas.comments,
        newRoot.metas.gamebook,
        newRoot.metas.glyphs,
        newRoot.tree.fold(Branches.empty)(_.toBranches),
        newRoot.metas.opening,
        newRoot.metas.clock,
        newRoot.metas.crazyData
      )

  extension (comments: Comments)
    def cleanup: Comments =
      Comments(comments.value.map(_.copy(id = Comment.Id("i"))))

  extension (node: NewBranch)
    def cleanup: NewBranch =
      node
        .focus(_.metas.comments)
        .modify(_.cleanup)
        .focus(_.path)
        .replace(UciPath.root)

  extension (root: NewRoot)
    def cleanup: NewRoot =
      root
        .focus(_.tree.some)
        .modify(_.map(_.cleanup))
        .focus(_.metas.comments)
        .modify(_.cleanup)

  def sanStr(node: PgnNode[NewBranch]): String = node.value.move.san.value
