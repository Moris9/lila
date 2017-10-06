package lila.study

import org.joda.time.DateTime
import scala.util.Try

import chess.format.pgn.{ Parser, ParsedPgn, Tag }
import lila.common.LightUser
import lila.game.{ Game, Namer }
import lila.round.JsonView.WithFlags
import lila.tree.Node.Comment
import lila.user.User

private final class ExplorerGame(
    importer: lila.explorer.ExplorerImporter,
    lightUser: LightUser.GetterSync,
    baseUrl: String
) {

  def quote(gameId: Game.ID): Fu[Option[Comment]] =
    importer(gameId) map {
      _ ?? { game =>
        gameComment(game).some
      }
    }

  def insert(userId: User.ID, study: Study, position: Position, gameId: Game.ID): Fu[Option[(Chapter, Path)]] =
    importer(gameId) map {
      _ ?? { game =>
        position.node ?? { fromNode =>
          GameToRoot(game, none, false).|> { root =>
            root.setCommentAt(
              comment = gameComment(game),
              path = Path(root.mainline.map(_.id))
            )
          } ?? { gameRoot =>
            merge(fromNode, position.path, gameRoot) flatMap {
              case (newNode, path) => position.chapter.addNode(newNode, path) map (_ -> path)
            }
          }
        }
      }
    }

  private def merge(fromNode: RootOrNode, fromPath: Path, game: Node.Root): Option[(Node, Path)] = {
    val gameNodes = game.mainline.dropWhile(_.fen != fromNode.fen) drop 1
    val (path, foundGameNode) = gameNodes.foldLeft((Path.root, none[Node])) {
      case ((path, None), gameNode) =>
        val nextPath = path + gameNode
        fromNode.children.nodeAt(nextPath) match {
          case Some(child) => (nextPath, none)
          case None => (path, gameNode.some)
        }
      case (found, _) => found
    }
    foundGameNode.map { _ -> fromPath.+(path) }
  }

  private def gameComment(game: Game) = Comment(
    id = Comment.Id.make,
    text = Comment.Text(s"${gameTitle(game)}, ${gameUrl(game)}"),
    by = Comment.Author.Lichess
  )

  private def gameUrl(game: Game) = s"$baseUrl/${game.id}"

  private def gameYear(pgn: Option[ParsedPgn], g: Game): Int =
    pgn.flatMap(_.tags.anyDate).flatMap { pgnDate =>
      Try(DateTime.parse(pgnDate, Tag.UTCDate.format)).toOption map (_.getYear)
    } | g.createdAt.getYear

  private def gameTitle(g: Game): String = {
    val pgn = g.pgnImport.flatMap(pgnImport => Parser.full(pgnImport.pgn).toOption)
    val white = pgn.flatMap(_.tags(_.White)) | Namer.playerText(g.whitePlayer)(lightUser)
    val black = pgn.flatMap(_.tags(_.Black)) | Namer.playerText(g.blackPlayer)(lightUser)
    val result = chess.Color.showResult(g.winnerColor)
    val event: String = {
      val raw = pgn.flatMap(_.tags(_.Event))
      val year = gameYear(pgn, g).toString
      raw.find(_ contains year) | raw.fold(year)(e => s"$e, $year")
    }
    s"$white - $black, $result, $event"
  }
}
