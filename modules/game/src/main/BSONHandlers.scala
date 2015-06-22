package lila.game

import lila.db.{ BSON, ByteArray }
import org.joda.time.DateTime
import reactivemongo.bson._

import chess.{ CheckCount, Color, Clock, White, Black, Status, Mode }
import chess.variant.Variant

object BSONHandlers {

  private[game] implicit val checkCountWriter = new BSONWriter[CheckCount, BSONArray] {
    def write(cc: CheckCount) = BSONArray(cc.white, cc.black)
  }

  implicit val gameBSONHandler = new BSON[Game] {

    import Game.BSONFields._
    import CastleLastMoveTime.castleLastMoveTimeBSONHandler
    import PgnImport.pgnImportBSONHandler
    import Player.playerBSONHandler

    private val emptyPlayerBuilder = playerBSONHandler.read(BSONDocument())

    def reads(r: BSON.Reader): Game = {
      val nbTurns = r int turns
      val winC = r boolO winnerColor map Color.apply
      val (whiteId, blackId) = r str playerIds splitAt 4
      val uids = ~r.getO[List[String]](playerUids)
      val (whiteUid, blackUid) = (uids.headOption.filter(_.nonEmpty), uids.lift(1).filter(_.nonEmpty))
      def player(field: String, color: Color, id: Player.Id, uid: Player.UserId): Player = {
        val builder = r.getO[Player.Builder](field)(playerBSONHandler) | emptyPlayerBuilder
        val win = winC map (_ == color)
        builder(color)(id)(uid)(win)
      }
      val createdAtValue = r date createdAt
      Game(
        id = r str id,
        whitePlayer = player(whitePlayer, White, whiteId, whiteUid),
        blackPlayer = player(blackPlayer, Black, blackId, blackUid),
        binaryPieces = r bytes binaryPieces,
        binaryPgn = r bytesD binaryPgn,
        status = Status(r int status) err "game invalid status",
        turns = nbTurns,
        startedAtTurn = r intD startedAtTurn,
        clock = r.getO[Color => Clock](clock)(clockBSONHandler(createdAtValue)) map (_(Color(0 == nbTurns % 2))),
        positionHashes = r.bytesD(positionHashes).value,
        checkCount = {
          val counts = r.intsD(checkCount)
          CheckCount(~counts.headOption, ~counts.lastOption)
        },
        castleLastMoveTime = r.get[CastleLastMoveTime](castleLastMoveTime)(castleLastMoveTimeBSONHandler),
        daysPerTurn = r intO daysPerTurn,
        binaryMoveTimes = (r bytesO moveTimes) | ByteArray.empty,
        mode = Mode(r boolD rated),
        variant = Variant(r intD variant) | chess.variant.Standard,
        next = r strO next,
        bookmarks = r intD bookmarks,
        createdAt = createdAtValue,
        updatedAt = r dateO updatedAt,
        metadata = Metadata(
          source = r intO source flatMap Source.apply,
          pgnImport = r.getO[PgnImport](pgnImport)(PgnImport.pgnImportBSONHandler),
          tournamentId = r strO tournamentId,
          simulId = r strO simulId,
          tvAt = r dateO tvAt,
          analysed = r boolD analysed)
      )
    }

    def writes(w: BSON.Writer, o: Game) = BSONDocument(
      id -> o.id,
      playerIds -> (o.whitePlayer.id + o.blackPlayer.id),
      playerUids -> w.listO(List(~o.whitePlayer.userId, ~o.blackPlayer.userId)),
      whitePlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.whitePlayer)),
      blackPlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.blackPlayer)),
      binaryPieces -> o.binaryPieces,
      binaryPgn -> w.byteArrayO(o.binaryPgn),
      status -> o.status.id,
      turns -> o.turns,
      startedAtTurn -> w.intO(o.startedAtTurn),
      clock -> (o.clock map { c => clockBSONHandler(o.createdAt).write(_ => c) }),
      positionHashes -> w.bytesO(o.positionHashes),
      checkCount -> o.checkCount.nonEmpty.option(o.checkCount),
      castleLastMoveTime -> castleLastMoveTimeBSONHandler.write(o.castleLastMoveTime),
      daysPerTurn -> o.daysPerTurn,
      moveTimes -> (BinaryFormat.moveTime write o.moveTimes),
      rated -> w.boolO(o.mode.rated),
      variant -> o.variant.exotic.option(o.variant.id).map(w.int),
      next -> o.next,
      bookmarks -> w.intO(o.bookmarks),
      createdAt -> w.date(o.createdAt),
      updatedAt -> o.updatedAt.map(w.date),
      source -> o.metadata.source.map(_.id),
      pgnImport -> o.metadata.pgnImport,
      tournamentId -> o.metadata.tournamentId,
      simulId -> o.metadata.simulId,
      tvAt -> o.metadata.tvAt.map(w.date),
      analysed -> w.boolO(o.metadata.analysed)
    )
  }

  import lila.db.ByteArray.ByteArrayBSONHandler

  def clockBSONHandler(since: DateTime) = new BSONHandler[BSONBinary, Color => Clock] {
    def read(bin: BSONBinary) = BinaryFormat clock since read {
      ByteArrayBSONHandler read bin
    }
    def write(clock: Color => Clock) = ByteArrayBSONHandler write {
      BinaryFormat clock since write clock(chess.White)
    }
  }
}
