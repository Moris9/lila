package lila.challenge

import chess.variant.Variant
import reactivemongo.api.bson._
import scala.util.Success

import lila.common.Days
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.{ *, given }

private object BSONHandlers {

  import Challenge._
  import lila.game.BSONHandlers.RulesHandler

  implicit val ColorChoiceBSONHandler = BSONIntegerHandler.as[ColorChoice](
    {
      case 1 => ColorChoice.White
      case 2 => ColorChoice.Black
      case _ => ColorChoice.Random
    },
    {
      case ColorChoice.White  => 1
      case ColorChoice.Black  => 2
      case ColorChoice.Random => 0
    }
  )
  implicit val TimeControlBSONHandler = new BSON[TimeControl] {
    import cats.implicits._
    def reads(r: Reader) =
      (r.intO("l"), r.intO("i")) mapN { (limit, inc) =>
        TimeControl.Clock(chess.Clock.Config(limit, inc))
      } orElse {
        r.getO[Days]("d") map TimeControl.Correspondence.apply
      } getOrElse TimeControl.Unlimited
    def writes(w: Writer, t: TimeControl) =
      t match {
        case TimeControl.Clock(chess.Clock.Config(l, i)) => $doc("l" -> l, "i" -> i)
        case TimeControl.Correspondence(d)               => $doc("d" -> d)
        case TimeControl.Unlimited                       => $empty
      }
  }
  implicit val VariantBSONHandler       = valueMapHandler(Variant.byId)(_.id)
  implicit val StatusBSONHandler        = valueMapHandler(Status.byId)(_.id)
  implicit val DeclineReasonBSONHandler = valueMapHandler(DeclineReason.byKey)(_.key)

  implicit val RatingBSONHandler = new BSON[Rating] {
    def reads(r: Reader) = Rating(r.int("i"), r.boolD("p"))
    def writes(w: Writer, r: Rating) =
      $doc(
        "i" -> r.int,
        "p" -> w.boolO(r.provisional)
      )
  }
  implicit val RegisteredBSONHandler = new BSON[Challenger.Registered] {
    def reads(r: Reader) = Challenger.Registered(r.str("id"), r.get[Rating]("r"))
    def writes(w: Writer, r: Challenger.Registered) =
      $doc(
        "id" -> r.id,
        "r"  -> r.rating
      )
  }
  implicit val AnonymousBSONHandler = new BSON[Challenger.Anonymous] {
    def reads(r: Reader)                           = Challenger.Anonymous(r.str("s"))
    def writes(w: Writer, a: Challenger.Anonymous) = $doc("s" -> a.secret)
  }
  implicit val ChallengerBSONHandler = new BSON[Challenger] {
    def reads(r: Reader) =
      if (r contains "id") RegisteredBSONHandler reads r
      else if (r contains "s") AnonymousBSONHandler reads r
      else Challenger.Open
    def writes(w: Writer, c: Challenger) =
      c match {
        case a: Challenger.Registered => RegisteredBSONHandler.writes(w, a)
        case a: Challenger.Anonymous  => AnonymousBSONHandler.writes(w, a)
        case _                        => $empty
      }
  }

  implicit private val OpenForIdsBSONHandler = tuple2BSONHandler[lila.user.User.ID]
  given BSONDocumentHandler[Challenge.Open] = Macros.handler
  given BSONDocumentHandler[Challenge] = Macros.handler
}
