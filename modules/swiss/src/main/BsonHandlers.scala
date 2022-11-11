package lila.swiss

import chess.Color
import chess.format.FEN
import reactivemongo.api.bson.*
import scala.concurrent.duration.*

import lila.db.BSON
import lila.db.dsl.{ *, given }
import lila.user.User

object BsonHandlers:

  given BSONHandler[chess.variant.Variant] = variantByKeyHandler
  given BSONHandler[chess.Clock.Config]    = clockConfigHandler
  given BSONHandler[Swiss.Points]          = intAnyValHandler(_.double, Swiss.Points.apply)
  given BSONHandler[Swiss.TieBreak]        = doubleAnyValHandler(_.value, Swiss.TieBreak.apply)
  given BSONHandler[Swiss.Performance]     = floatAnyValHandler(_.value, Swiss.Performance.apply)
  given BSONHandler[Swiss.Score]           = intAnyValHandler(_.value, Swiss.Score.apply)
  given idHandler: BSONHandler[Swiss.Id]   = stringAnyValHandler(_.value, Swiss.Id.apply)
  given BSONHandler[SwissRound.Number]     = intAnyValHandler(_.value, SwissRound.Number.apply)
  given BSONHandler[SwissPlayer.Id]        = stringAnyValHandler(_.value, SwissPlayer.Id.apply)

  given BSON[SwissPlayer] with
    import SwissPlayer.Fields.*
    def reads(r: BSON.Reader) =
      SwissPlayer(
        id = r.get[SwissPlayer.Id](id),
        swissId = r.get[Swiss.Id](swissId),
        userId = r str userId,
        rating = r int rating,
        provisional = r boolD provisional,
        points = r.get[Swiss.Points](points),
        tieBreak = r.get[Swiss.TieBreak](tieBreak),
        performance = r.getO[Swiss.Performance](performance),
        score = r.get[Swiss.Score](score),
        absent = r.boolD(absent),
        byes = ~r.getO[Set[SwissRound.Number]](byes)
      )
    def writes(w: BSON.Writer, o: SwissPlayer) =
      $doc(
        id          -> o.id,
        swissId     -> o.swissId,
        userId      -> o.userId,
        rating      -> o.rating,
        provisional -> w.boolO(o.provisional),
        points      -> o.points,
        tieBreak    -> o.tieBreak,
        performance -> o.performance,
        score       -> o.score,
        absent      -> w.boolO(o.absent),
        byes        -> o.byes.some.filter(_.nonEmpty)
      )

  given BSONHandler[SwissPairing.Status] = lila.db.dsl.quickHandler(
    {
      case BSONBoolean(true)  => Left(SwissPairing.Ongoing)
      case BSONInteger(index) => Right(Color.fromWhite(index == 0).some)
      case _                  => Right(none)
    },
    {
      case Left(_)        => BSONBoolean(true)
      case Right(Some(c)) => BSONInteger(c.fold(0, 1))
      case _              => BSONNull
    }
  )
  given BSON[SwissPairing] with
    import SwissPairing.Fields.*
    def reads(r: BSON.Reader) =
      r.get[List[User.ID]](players) match
        case List(w, b) =>
          SwissPairing(
            id = r str id,
            swissId = r.get[Swiss.Id](swissId),
            round = r.get[SwissRound.Number](round),
            white = w,
            black = b,
            status = r.getO[SwissPairing.Status](status) | Right(none),
            isForfeit = r.boolD(isForfeit)
          )
        case _ => sys error "Invalid swiss pairing users"
    def writes(w: BSON.Writer, o: SwissPairing) =
      $doc(
        id        -> o.id,
        swissId   -> o.swissId,
        round     -> o.round,
        players   -> o.players,
        status    -> o.status,
        isForfeit -> w.boolO(o.isForfeit)
      )

  import SwissCondition.BSONHandlers.given

  given BSON[Swiss.Settings] with
    def reads(r: BSON.Reader) =
      Swiss.Settings(
        nbRounds = r.get[Int]("n"),
        rated = r.boolO("r") | true,
        description = r.strO("d"),
        position = r.getO[FEN]("f"),
        chatFor = r.intO("c") | Swiss.ChatFor.default,
        roundInterval = (r.intO("i") | 60).seconds,
        password = r.strO("p"),
        conditions = r.getO[SwissCondition.All]("o") getOrElse SwissCondition.All.empty,
        forbiddenPairings = r.getD[String]("fp"),
        manualPairings = r.getD[String]("mp")
      )
    def writes(w: BSON.Writer, s: Swiss.Settings) =
      $doc(
        "n"  -> s.nbRounds,
        "r"  -> (!s.rated).option(false),
        "d"  -> s.description,
        "f"  -> s.position,
        "c"  -> (s.chatFor != Swiss.ChatFor.default).option(s.chatFor),
        "i"  -> s.roundInterval.toSeconds.toInt,
        "p"  -> s.password,
        "o"  -> s.conditions.ifNonEmpty,
        "fp" -> s.forbiddenPairings.some.filter(_.nonEmpty),
        "mp" -> s.manualPairings.some.filter(_.nonEmpty)
      )

  given BSONDocumentHandler[Swiss] = Macros.handler

  // "featurable" mostly means that the tournament isn't over yet
  def addFeaturable(s: Swiss): Bdoc =
    bsonWriteObjTry[Swiss](s).get ++ {
      s.isNotFinished ?? $doc(
        "featurable" -> true,
        "garbage"    -> s.unrealisticSettings.option(true)
      )
    }

  import Swiss.IdName
  given BSONDocumentHandler[IdName]   = Macros.handler
  given BSONDocumentHandler[SwissBan] = Macros.handler
