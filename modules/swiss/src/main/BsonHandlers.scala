package lila.swiss

import chess.Clock.{ Config => ClockConfig }
import chess.variant.Variant
import chess.StartingPosition
import lila.db.BSON
import lila.db.dsl._
import reactivemongo.api.bson._

private object BsonHandlers {

  implicit val clockHandler = tryHandler[ClockConfig](
    {
      case doc: BSONDocument =>
        for {
          limit <- doc.getAsTry[Int]("limit")
          inc   <- doc.getAsTry[Int]("increment")
        } yield ClockConfig(limit, inc)
    },
    c =>
      BSONDocument(
        "limit"     -> c.limitSeconds,
        "increment" -> c.incrementSeconds
      )
  )
  implicit val variantHandler = lila.db.dsl.quickHandler[Variant](
    {
      case BSONString(v) => Variant orDefault v
      case _             => Variant.default
    },
    v => BSONString(v.key)
  )
  private lazy val fenIndex: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.fen -> p
  }.toMap
  implicit val startingPositionHandler = lila.db.dsl.quickHandler[StartingPosition](
    {
      case BSONString(v) => fenIndex.getOrElse(v, StartingPosition.initial)
      case _             => StartingPosition.initial
    },
    v => BSONString(v.fen)
  )
  implicit val swissPointsHandler   = intAnyValHandler[Swiss.Points](_.double, Swiss.Points.apply)
  implicit val swissTieBreakHandler = doubleAnyValHandler[Swiss.TieBreak](_.value, Swiss.TieBreak.apply)
  implicit val swissPerformanceHandler =
    floatAnyValHandler[Swiss.Performance](_.value, Swiss.Performance.apply)
  implicit val swissScoreHandler   = intAnyValHandler[Swiss.Score](_.value, Swiss.Score.apply)
  implicit val playerNumberHandler = intAnyValHandler[SwissPlayer.Number](_.value, SwissPlayer.Number.apply)
  implicit val roundNumberHandler  = intAnyValHandler[SwissRound.Number](_.value, SwissRound.Number.apply)
  implicit val swissIdHandler      = stringAnyValHandler[Swiss.Id](_.value, Swiss.Id.apply)
  implicit val playerIdHandler     = stringAnyValHandler[SwissPlayer.Id](_.value, SwissPlayer.Id.apply)

  implicit val playerHandler = new BSON[SwissPlayer] {
    import SwissPlayer.Fields._
    def reads(r: BSON.Reader) = SwissPlayer(
      id = r.get[SwissPlayer.Id](id),
      swissId = r.get[Swiss.Id](swissId),
      number = r.get[SwissPlayer.Number](number),
      userId = r str userId,
      rating = r int rating,
      provisional = r boolD provisional,
      points = r.get[Swiss.Points](points),
      tieBreak = r.get[Swiss.TieBreak](tieBreak),
      performance = r.getO[Swiss.Performance](performance),
      score = r.get[Swiss.Score](score),
      absent = r.boolD(absent)
    )
    def writes(w: BSON.Writer, o: SwissPlayer) = $doc(
      id          -> o.id,
      swissId     -> o.swissId,
      number      -> o.number,
      userId      -> o.userId,
      rating      -> o.rating,
      provisional -> w.boolO(o.provisional),
      points      -> o.points,
      tieBreak    -> o.tieBreak,
      performance -> o.performance,
      score       -> o.score,
      absent      -> w.boolO(o.absent)
    )
  }

  implicit val pairingStatusHandler = lila.db.dsl.quickHandler[SwissPairing.Status](
    {
      case BSONInteger(n)    => Right(SwissPlayer.Number(n).some)
      case BSONBoolean(true) => Left(SwissPairing.Ongoing)
      case _                 => Right(none)
    }, {
      case Left(_)        => BSONBoolean(true)
      case Right(Some(n)) => BSONInteger(n.value)
      case _              => BSONNull
    }
  )
  implicit val pairingHandler = new BSON[SwissPairing] {
    import SwissPairing.Fields._
    def reads(r: BSON.Reader) =
      r.get[List[SwissPlayer.Number]](players) match {
        case List(w, b) =>
          SwissPairing(
            id = r str id,
            swissId = r.get[Swiss.Id](swissId),
            round = r.get[SwissRound.Number](round),
            white = w,
            black = b,
            status = r.get[SwissPairing.Status](status)
          )
        case _ => sys error "Invalid swiss pairing users"
      }
    def writes(w: BSON.Writer, o: SwissPairing) = $doc(
      id      -> o.id,
      swissId -> o.swissId,
      round   -> o.round,
      gameId  -> o.gameId,
      players -> o.players,
      status  -> o.status
    )
  }

  implicit val swissHandler = Macros.handler[Swiss]
}
