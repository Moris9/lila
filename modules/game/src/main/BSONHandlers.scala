package lila.game

import chess.variant.{ Crazyhouse, Variant }
import chess.{
  Ply,
  HalfMoveClock,
  CheckCount,
  Clock,
  Color,
  Game as ChessGame,
  History as ChessHistory,
  Mode,
  Status,
  UnmovedRooks
}
import chess.format.Fen
import org.joda.time.DateTime
import reactivemongo.api.bson.*
import scala.util.{ Success, Try }

import lila.db.BSON
import lila.db.dsl.{ *, given }
import lila.common.Days

object BSONHandlers:

  import lila.db.ByteArray.byteArrayHandler

  private[game] given checkCountWriter: BSONWriter[CheckCount] with
    def writeTry(cc: CheckCount) = Success(BSONArray(cc.white, cc.black))

  given BSONHandler[Status] = tryHandler[Status](
    { case BSONInteger(v) => Status(v) toTry s"No such status: $v" },
    x => BSONInteger(x.id)
  )

  private[game] given BSONHandler[UnmovedRooks] = tryHandler[UnmovedRooks](
    { case bin: BSONBinary => byteArrayHandler.readTry(bin) map BinaryFormat.unmovedRooks.read },
    x => byteArrayHandler.writeTry(BinaryFormat.unmovedRooks write x).get
  )

  given BSONHandler[GameRule] = valueMapHandler(GameRule.byKey)(_.key)

  private[game] given crazyhouseDataHandler: BSON[Crazyhouse.Data] with
    import Crazyhouse.*
    def reads(r: BSON.Reader) =
      Crazyhouse.Data(
        pockets = {
          val (white, black) = {
            r.str("p").view.flatMap(chess.Piece.fromChar).to(List)
          }.partition(_ is chess.White)
          Pockets(
            white = Pocket(white.map(_.role)),
            black = Pocket(black.map(_.role))
          )
        },
        promoted = r.str("t").view.flatMap(chess.Pos.fromChar(_)).to(Set)
      )
    def writes(w: BSON.Writer, o: Crazyhouse.Data) =
      BSONDocument(
        "p" -> {
          o.pockets.white.roles.map(_.forsythUpper).mkString +
            o.pockets.black.roles.map(_.forsyth).mkString
        },
        "t" -> o.promoted.map(_.asChar).mkString
      )

  private[game] given gameDrawOffersHandler: BSONHandler[GameDrawOffers] = tryHandler[GameDrawOffers](
    { case arr: BSONArray =>
      Success(arr.values.foldLeft(GameDrawOffers.empty) {
        case (offers, BSONInteger(p)) =>
          if (p > 0) offers.copy(white = offers.white incl Ply(p))
          else offers.copy(black = offers.black incl Ply(-p))
        case (offers, _) => offers
      })
    },
    offers =>
      BSONArray(
        (Ply.raw(offers.white) ++ Ply.raw(offers.black).map(-_)).view.map(BSONInteger.apply).toIndexedSeq
      )
  )

  given gameBSONHandler: BSON[Game] with
    import Game.BSONFields as F
    import PgnImport.given
    def reads(r: BSON.Reader): Game =

      lila.mon.game.fetch.increment()

      val light        = lightGameHandler.readsWithPlayerIds(r, r str F.playerIds)
      val startedAtPly = Ply(r intD F.startedAtTurn)
      val ply          = r.get[Ply](F.turns) atMost Game.maxPlies // unlimited can cause StackOverflowError
      val turnColor    = ply.color
      val createdAt    = r date F.createdAt

      val playedPlies = ply - startedAtPly
      val gameVariant = Variant.idOrDefault(r.getO[Variant.Id](F.variant))

      val decoded = r.bytesO(F.huffmanPgn).map { PgnStorage.Huffman.decode(_, playedPlies) } | {
        val clm  = r.get[CastleLastMove](F.castleLastMove)
        val sans = PgnStorage.OldBin.decode(r bytesD F.oldPgn, playedPlies)
        val halfMoveClock =
          HalfMoveClock from sans.reverse
            .indexWhere(san => san.value.contains("x") || san.value.headOption.exists(_.isLower))
            .some
            .filter(0 <= _)
        PgnStorage.Decoded(
          sans = sans,
          pieces = BinaryFormat.piece.read(r bytes F.binaryPieces, gameVariant),
          positionHashes = r.getD[chess.PositionHash](F.positionHashes),
          unmovedRooks = r.getO[UnmovedRooks](F.unmovedRooks) | UnmovedRooks.default,
          lastMove = clm.lastMove,
          castles = clm.castles,
          halfMoveClock = halfMoveClock orElse
            r.getO[Fen.Epd](F.initialFen).flatMap { fen =>
              Fen.readHalfMoveClockAndFullMoveNumber(fen)._1
            } getOrElse playedPlies.into(HalfMoveClock)
        )
      }
      val chessGame = ChessGame(
        situation = chess.Situation(
          chess.Board(
            pieces = decoded.pieces,
            history = ChessHistory(
              lastMove = decoded.lastMove,
              castles = decoded.castles,
              halfMoveClock = decoded.halfMoveClock,
              positionHashes = decoded.positionHashes,
              unmovedRooks = decoded.unmovedRooks,
              checkCount = if (gameVariant.threeCheck) {
                val counts = r.intsD(F.checkCount)
                CheckCount(~counts.headOption, ~counts.lastOption)
              } else Game.emptyCheckCount
            ),
            variant = gameVariant,
            crazyData = gameVariant.crazyhouse option r.get[Crazyhouse.Data](F.crazyData)
          ),
          color = turnColor
        ),
        sans = decoded.sans,
        clock = r.getO[Color => Clock](F.clock)(using
          clockBSONReader(createdAt, light.whitePlayer.berserk, light.blackPlayer.berserk)
        ) map (_(turnColor)),
        ply = ply,
        startedAtPly = startedAtPly
      )

      val whiteClockHistory = r bytesO F.whiteClockHistory
      val blackClockHistory = r bytesO F.blackClockHistory

      Game(
        id = light.id,
        whitePlayer = light.whitePlayer,
        blackPlayer = light.blackPlayer,
        chess = chessGame,
        loadClockHistory = clk =>
          for {
            bw <- whiteClockHistory
            bb <- blackClockHistory
            history <-
              BinaryFormat.clockHistory
                .read(clk.limit, bw, bb, (light.status == Status.Outoftime).option(turnColor))
            _ = lila.mon.game.loadClockHistory.increment()
          } yield history,
        status = light.status,
        daysPerTurn = r.getO[Days](F.daysPerTurn),
        binaryMoveTimes = r bytesO F.moveTimes,
        mode = Mode(r boolD F.rated),
        bookmarks = r intD F.bookmarks,
        createdAt = createdAt,
        movedAt = r.dateD(F.movedAt, createdAt),
        metadata = Metadata(
          source = r intO F.source flatMap Source.apply,
          pgnImport = r.getO[PgnImport](F.pgnImport),
          tournamentId = r.getO[TourId](F.tournamentId),
          swissId = r.getO[SwissId](F.swissId),
          simulId = r.getO[SimulId](F.simulId),
          analysed = r boolD F.analysed,
          drawOffers = r.getD(F.drawOffers, GameDrawOffers.empty),
          rules = r.getD(F.rules, Set.empty)
        )
      )

    def writes(w: BSON.Writer, o: Game) =
      BSONDocument(
        F.id        -> o.id,
        F.playerIds -> (o.whitePlayer.id.value + o.blackPlayer.id.value),
        F.playerUids -> ((o.whitePlayer.userId, o.blackPlayer.userId) match
          case (None, None)    => None
          case (Some(w), None) => Some(List(w.value))
          case (wo, Some(b))   => Some(List(wo.??(_.value), b.value))
        ),
        F.whitePlayer   -> w.docO(Player.playerWrite(o.whitePlayer)),
        F.blackPlayer   -> w.docO(Player.playerWrite(o.blackPlayer)),
        F.status        -> o.status,
        F.turns         -> o.chess.ply,
        F.startedAtTurn -> w.intO(o.chess.startedAtPly.value),
        F.clock -> (o.chess.clock flatMap { c =>
          clockBSONWrite(o.createdAt, c).toOption
        }),
        F.daysPerTurn       -> o.daysPerTurn,
        F.moveTimes         -> o.binaryMoveTimes,
        F.whiteClockHistory -> clockHistory(Color.White, o.clockHistory, o.chess.clock, o.flagged),
        F.blackClockHistory -> clockHistory(Color.Black, o.clockHistory, o.chess.clock, o.flagged),
        F.rated             -> w.boolO(o.mode.rated),
        F.variant           -> o.board.variant.exotic.option(w(o.board.variant.id)),
        F.bookmarks         -> w.intO(o.bookmarks),
        F.createdAt         -> w.date(o.createdAt),
        F.movedAt           -> w.date(o.movedAt),
        F.source            -> o.metadata.source.map(_.id),
        F.pgnImport         -> o.metadata.pgnImport,
        F.tournamentId      -> o.metadata.tournamentId,
        F.swissId           -> o.metadata.swissId,
        F.simulId           -> o.metadata.simulId,
        F.analysed          -> w.boolO(o.metadata.analysed),
        F.rules             -> o.metadata.nonEmptyRules
      ) ++ {
        if (o.variant.standard)
          $doc(F.huffmanPgn -> PgnStorage.Huffman.encode(o.sans take Game.maxPlies.value))
        else
          val f = PgnStorage.OldBin
          $doc(
            F.oldPgn         -> f.encode(o.sans take Game.maxPlies.value),
            F.binaryPieces   -> BinaryFormat.piece.write(o.board.pieces),
            F.positionHashes -> o.history.positionHashes,
            F.unmovedRooks   -> o.history.unmovedRooks,
            F.castleLastMove -> CastleLastMove(castles = o.history.castles, lastMove = o.history.lastMove),
            F.checkCount     -> o.history.checkCount.nonEmpty.option(o.history.checkCount),
            F.crazyData      -> o.board.crazyData
          )
      }

  given lightGameHandler: lila.db.BSONReadOnly[LightGame] with

    import Game.BSONFields as F

    private val emptyPlayerBuilder = Player.builderRead($empty)

    def reads(r: BSON.Reader): LightGame =
      lila.mon.game.fetchLight.increment()
      readsWithPlayerIds(r, "")

    def readsWithPlayerIds(r: BSON.Reader, playerIds: String): LightGame =
      val (whiteId, blackId)   = playerIds splitAt 4
      val winC                 = r boolO F.winnerColor map { Color.fromWhite(_) }
      val uids                 = ~r.getO[List[UserId]](F.playerUids)
      val (whiteUid, blackUid) = (uids.headOption.filter(_.value.nonEmpty), uids.lift(1))
      def makePlayer(field: String, color: Color, id: String, uid: Option[UserId]): Player =
        val builder = r.getO[Player.Builder](field)(using Player.playerReader) | emptyPlayerBuilder
        builder(color)(GamePlayerId(id))(uid)(winC map (_ == color))
      LightGame(
        id = r.get[GameId](F.id),
        whitePlayer = makePlayer(F.whitePlayer, Color.White, whiteId, whiteUid),
        blackPlayer = makePlayer(F.blackPlayer, Color.Black, blackId, blackUid),
        status = r.get[Status](F.status)
      )

  private def clockHistory(
      color: Color,
      clockHistory: Option[ClockHistory],
      clock: Option[Clock],
      flagged: Option[Color]
  ) =
    for {
      clk     <- clock
      history <- clockHistory
      times = history(color)
    } yield BinaryFormat.clockHistory.writeSide(clk.limit, times, flagged has color)

  private[game] def clockBSONReader(since: DateTime, whiteBerserk: Boolean, blackBerserk: Boolean) =
    new BSONReader[Color => Clock]:
      def readTry(bson: BSONValue): Try[Color => Clock] =
        bson match
          case bin: BSONBinary =>
            byteArrayHandler readTry bin map { cl =>
              BinaryFormat.clock(since).read(cl, whiteBerserk, blackBerserk)
            }
          case b => lila.db.BSON.handlerBadType(b)

  private[game] def clockBSONWrite(since: DateTime, clock: Clock) =
    byteArrayHandler writeTry {
      BinaryFormat clock since write clock
    }
