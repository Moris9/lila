package lila.study

import chess.opening._
import lila.tree

object TreeBuilder {

  private val initialStandardDests = chess.Game(chess.variant.Standard).situation.destinations

  def apply(root: Node.Root, variant: chess.variant.Variant) = {
    val dests =
      if (variant.standard && root.fen.value == chess.format.Forsyth.initial) initialStandardDests
      else {
        val sit = chess.Game(variant.some, root.fen.value.some).situation
        sit.playable(false) ?? sit.destinations
      }
    makeRoot(root).copy(dests = dests.some)
  }

  def makeRoot(root: Node.Root) = tree.Root(
    ply = root.ply,
    fen = root.fen.value,
    check = root.check,
    shapes = root.shapes,
    comments = root.comments,
    glyphs = root.glyphs,
    crazyData = root.crazyData,
    children = toBranches(root.children),
    opening = FullOpeningDB findByFen root.fen.value
  )

  def toBranch(node: Node): tree.Branch = tree.Branch(
    id = node.id,
    ply = node.ply,
    move = node.move,
    fen = node.fen.value,
    check = node.check,
    shapes = node.shapes,
    comments = node.comments,
    glyphs = node.glyphs,
    crazyData = node.crazyData,
    children = toBranches(node.children),
    opening = FullOpeningDB findByFen node.fen.value
  )

  private def toBranches(children: Node.Children): List[tree.Branch] =
    children.nodes.map(toBranch)(scala.collection.breakOut)
}
