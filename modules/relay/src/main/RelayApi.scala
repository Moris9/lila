package lila.relay

import chess.format.pgn.Tag
import lila.socket.Socket.Uid
import lila.study._

private final class RelayApi(
    studyApi: StudyApi,
    chapterRepo: ChapterRepo
) {

  val currents = List(
    Relay(
      lila.study.Study.Id("AoUZ6bOS"),
      url = "http://localhost:3000"
    )
  )

  def sync(relay: Relay, multiPgn: String): Funit = studyApi byId relay.studyId flatMap {
    _ ?? { study =>
      chapterRepo orderedByStudy study.id flatMap { chapters =>
        multiGamePgnToGames(multiPgn, logger.branch(relay.toString)).map { game =>
          chapters.find(_.tags("RelayId") contains game.id) match {
            case Some(chapter) => updateChapter(study, chapter, game)
            case None => createChapter(study, game)
          }
        }.sequenceFu.void
      }
    }
  }

  private val moveOpts = MoveOpts(
    write = true,
    sticky = false,
    promoteToMainline = true,
    clock = none
  )

  private val socketUid = Uid("")

  private def updateChapter(study: Study, chapter: Chapter, game: RelayGame): Funit = {
    game.root.mainline.foldLeft(Path.root -> none[Node]) {
      case ((parentPath, None), gameNode) =>
        val path = parentPath + gameNode
        if (chapter.root.nodeAt(path).isDefined) path -> none
        else (parentPath -> gameNode.some)
      case (found, _) => found
    } match {
      case (_, None) => funit
      case (path, Some(node)) =>
        lila.common.Future.fold(node.mainline)(Position(chapter, path)) {
          case (position, n) => studyApi.doAddNode(
            userId = position.chapter.ownerId,
            study = study,
            position = position,
            node = n,
            uid = socketUid,
            opts = moveOpts.copy(clock = n.clock)
          ) flatten s"Can't add relay node $position $node"
        } void
    }
  }

  private def createChapter(study: Study, game: RelayGame): Funit =
    chapterRepo.nextOrderByStudy(study.id) flatMap { order =>
      val chapter = Chapter.make(
        studyId = study.id,
        name = Chapter.Name(s"${game.whiteName} - ${game.blackName}"),
        setup = Chapter.Setup(
          none,
          game.tags.variant | chess.variant.Variant.default,
          chess.Color.White
        ),
        root = game.root,
        tags = game.tags + Tag("RelayId", game.id),
        order = order,
        ownerId = study.ownerId,
        practice = false,
        gamebook = false,
        conceal = none
      )
      studyApi.doAddChapter(study, chapter, sticky = false, uid = socketUid)
    }

  private def multiGamePgnToGames(multiPgn: String, logger: lila.log.Logger): List[RelayGame] =
    splitGames(multiPgn).flatMap { pgn =>
      PgnImport(pgn, Nil).fold(
        err => {
          logger.info(s"Invalid PGN $err")
          none
        },
        res => for {
          white <- res.tags(_.White)
          black <- res.tags(_.Black)
        } yield RelayGame(
          tags = res.tags,
          root = res.root,
          whiteName = RelayGame.PlayerName(white),
          blackName = RelayGame.PlayerName(black)
        )
      )
    }

  private def splitGames(multiPgn: String): List[String] =
    """\n\n\[""".r.split(multiPgn.replace("\r\n", "\n")).toList match {
      case first :: rest => first :: rest.map(t => s"[$t")
      case Nil => Nil
    }
}
