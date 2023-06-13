package lila.tree

import alleycats.Zero
import cats.syntax.all.*
import monocle.syntax.all.*
import chess.{ Centis, HasId }
import chess.{ Node as ChessNode, Variation }
import chess.Node.*
import chess.format.pgn.{ Glyph, Glyphs }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.opening.Opening
import chess.{ Ply, Square, Check }
import chess.variant.{ Variant, Crazyhouse }
import play.api.libs.json.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.Json.{ given, * }

import Node.{ Comments, Comment, Gamebook, Shapes }
import chess.Mergeable
import chess.Tree
import scala.annotation.targetName
import chess.Situation

case class Metas(
    ply: Ply,
    fen: Fen.Epd,
    check: Check,
    // None when not computed yet
    dests: Option[Map[Square, List[Square]]] = None,
    drops: Option[List[Square]] = None,
    eval: Option[Eval] = None,
    shapes: Node.Shapes = Shapes.empty,
    comments: Node.Comments = Comments.empty,
    gamebook: Option[Node.Gamebook] = None,
    glyphs: Glyphs = Glyphs.empty,
    opening: Option[Opening] = None,
    clock: Option[Centis] = None,
    crazyData: Option[Crazyhouse.Data] = None
    // TODO, add support for variationComments
):
  def setComment(comment: Comment) = copy(comments = comments.set(comment))
  def deleteComment(comment: Comment.Id) =
    copy(comments = comments.delete(comment))
  def toggleGlyph(glyph: Glyph) = copy(glyphs = glyphs toggle glyph)
  def turn                      = ply.turn

object Metas:
  def default(variant: Variant): Metas =
    Metas(
      ply = Ply.initial,
      fen = variant.initialFen,
      check = Check.No,
      crazyData = variant.crazyhouse option Crazyhouse.Data.init
    )
  def apply(sit: Situation.AndFullMoveNumber): Metas =
    Metas(
      ply = sit.ply,
      fen = Fen write sit,
      check = sit.situation.check,
      clock = none,
      crazyData = sit.situation.board.crazyData
    )

case class NewBranch(
    id: UciCharPair,
    // additional data to make searching with path easier
    path: UciPath,
    move: Uci.WithSan,
    comp: Boolean = false, // generated by a computer analysis
    forceVariation: Boolean = false,
    metas: Metas
):
  export metas.{
    ply,
    fen,
    check,
    dests,
    drops,
    eval,
    shapes,
    comments,
    gamebook,
    glyphs,
    opening,
    clock,
    crazyData
  }
  override def toString                    = s"$ply, $id, ${move.uci}"
  def withClock(centis: Option[Centis])    = this.focus(_.metas.clock).set(centis)
  def withForceVariation(force: Boolean)   = copy(forceVariation = force)
  def isCommented                          = metas.comments.value.nonEmpty
  def setComment(comment: Comment)         = this.focus(_.metas).modify(_.setComment(comment))
  def deleteComment(commentId: Comment.Id) = this.focus(_.metas).modify(_.deleteComment(commentId))
  def deleteComments                       = this.focus(_.metas.comments).set(Comments.empty)
  def setGamebook(gamebook: Gamebook)      = this.focus(_.metas.gamebook).set(gamebook.some)
  def setShapes(s: Shapes)                 = this.focus(_.metas.shapes).set(s)
  def toggleGlyph(glyph: Glyph)            = this.focus(_.metas).modify(_.toggleGlyph(glyph))
  def clearAnnotations = this.focus(_.metas).modify(_.copy(shapes = Shapes.empty, glyphs = Glyphs.empty))
  def setComp          = copy(comp = true)
  def merge(n: NewBranch): NewBranch =
    copy(
      metas = metas.copy(
        shapes = metas.shapes ++ n.metas.shapes,
        comments = metas.comments ++ n.metas.comments,
        gamebook = n.metas.gamebook orElse metas.gamebook,
        glyphs = metas.glyphs merge n.metas.glyphs,
        eval = n.metas.eval orElse metas.eval,
        clock = n.metas.clock orElse metas.clock,
        crazyData = n.metas.crazyData orElse metas.crazyData
      ),
      forceVariation = n.forceVariation || forceVariation
    )

object NewBranch:
  given HasId[NewBranch, UciCharPair] = _.id
  given Mergeable[NewBranch]          = _.merge(_)

type NewTree = ChessNode[NewBranch]

object NewTree:
  // default case class constructor not working with type alias?
  def apply(value: NewBranch, child: Option[NewTree], variations: List[Variation[NewBranch]]) =
    ChessNode(value, child, variations)

  def apply(root: Root): Option[NewTree] =
    root.children.first.map: first =>
      NewTree(
        value = fromBranch(first),
        child = first.children.first.map(fromBranch(_, first.children.variations)),
        variations = root.children.variations.map(toVariation)
      )

  def fromBranch(branch: Branch, variations: List[Branch]): NewTree =
    NewTree(
      value = fromBranch(branch),
      child = branch.children.first.map(fromBranch(_, branch.children.variations)),
      variations = variations.map(toVariation)
    )

  def toVariation(branch: Branch): Variation[NewBranch] =
    Variation(
      value = fromBranch(branch),
      child = branch.children.first.map(fromBranch(_, branch.children.variations))
    )

  def fromBranch(branch: Branch): NewBranch =
    NewBranch(
      branch.id,
      UciPath.root,
      branch.move,
      branch.comp,
      branch.forceVariation,
      fromNode(branch)
    )

  def fromNode(node: Node) =
    Metas(
      node.ply,
      node.fen,
      node.check,
      node.dests,
      node.drops,
      node.eval,
      node.shapes,
      node.comments,
      node.gamebook,
      node.glyphs,
      node.opening,
      node.clock,
      node.crazyData
    )

  // given defaultNodeJsonWriter: Writes[NewTree] = makeNodeJsonWriter(alwaysChildren = true)
  // def minimalNodeJsonWriter: Writes[NewTree]   = makeNodeJsonWriter(alwaysChildren = false)
  // def makeNodeJsonWriter(alwaysChildren: Boolean): Writes[NewTree] = ?so
  // Optional for the first node with the given id
  // def filterById(id: UciCharPair) = ChessNode.filterOptional[NewBranch](_.id == id)

case class NewRoot(metas: Metas, tree: Option[NewTree]):
  import NewRoot.*

  export metas.{
    ply,
    fen,
    check,
    dests,
    drops,
    eval,
    shapes,
    comments,
    gamebook,
    glyphs,
    opening,
    clock,
    crazyData
  }
  def mainlineValues: List[NewBranch] = tree.fold(List.empty[NewBranch])(_.mainlineValues)

  def mapChild(f: NewBranch => NewBranch): NewRoot =
    copy(tree = tree.map(_.map(f)))

  def pathExists(path: UciPath): Boolean =
    path.isEmpty || tree.exists(_.pathExists(path.ids))

  def addNodeAt(path: UciPath, node: NewTree): Option[NewRoot] =
    if tree.isEmpty && path.isEmpty then copy(tree = node.some).some
    else tree.flatMap(_.addNodeAt(path.ids)(node)).map(x => copy(tree = x.some))

  def modifyWithParentPathMetas(path: UciPath, f: Metas => Metas): Option[NewRoot] =
    if tree.isEmpty && path.isEmpty then copy(metas = f(metas)).some
    else
      tree.flatMap:
        _.modifyChildAt(path.ids, _.focus(_.value.metas).modify(f).some).map(x => copy(tree = x.some))

  def modifyWithParentPath(path: UciPath, f: NewBranch => NewBranch): Option[NewRoot] =
    if tree.isEmpty && path.isEmpty then this.some
    else
      tree.flatMap:
        _.modifyChildAt(path.ids, x => x.copy(value = f(x.value)).some).map(x => copy(tree = x.some))

  def withoutChildren: NewRoot = copy(tree = None)
  def withTree(t: Option[NewTree]): NewRoot =
    copy(tree = t)

  def isEmpty = tree.isEmpty

  def size = tree.fold(0L)(_.size)

  def mainlinePath = tree.fold(UciPath.root)(x => UciPath.fromIds(x.mainlinePath.toIterable))

  def lastMainlineNode         = tree.map(_.lastMainlineNode)
  def lastMainlineMetas        = lastMainlineNode.map(_.value.metas)
  def lastMainlineMetasOrRoots = lastMainlineMetas | metas
  def modifyLastMainlineOrRoot(f: Metas => Metas): NewRoot =
    tree.fold(copy(metas = f(metas))): tree =>
      copy(tree = tree.modifyLastMainlineNode(_.withValue(_.focus(_.metas).modify(f))).some)

  override def toString = s"$tree"

object NewRoot:
  def default(variant: Variant)                        = NewRoot(Metas.default(variant), None)
  def apply(sit: Situation.AndFullMoveNumber): NewRoot = NewRoot(Metas(sit), None)
  extension (path: UciPath) def ids                    = path.computeIds.toList

  given defaultNodeJsonWriter: Writes[NewRoot] = makeNodeJsonWriter(alwaysChildren = true)
  def minimalNodeJsonWriter                    = makeNodeJsonWriter(alwaysChildren = false)
  def makeNodeJsonWriter(alwaysChildren: Boolean): Writes[NewRoot] = ???
