package lila.tv

import akka.actor._
import scala.concurrent.duration._

import chess.Color
import lila.game.{ Game, GameRepo }

private[tv] final class ChannelActor(channel: Tv.Channel) extends Actor {

  import ChannelActor._

  implicit private def timeout = makeTimeout(50 millis)

  private var oneId = none[String]

  def receive = {

    case GetGame => sender ! oneId

    case SetGame(game) =>
      oneId = game.id.some
      context.parent ! TvActor.Selected(channel, game)

    case Select(candidates) => if (candidates.nonEmpty) {
      oneId ?? GameRepo.game foreach {
        case Some(game) if channel.filter(game) =>
          wayBetter(game, candidates) foreach elect
        case Some(game) => rematch(game) orElse feature(candidates) foreach elect
        case _          => feature(candidates) foreach elect
      }
    }
  }

  def elect(gameOption: Option[Game]) {
    gameOption foreach { self ! SetGame(_) }
  }

  def wayBetter(game: Game, candidates: List[Game]) = feature(candidates) map {
    case Some(next) if isWayBetter(game, next) => next.some
    case _                                     => none
  }

  def isWayBetter(g1: Game, g2: Game) = score(g2.resetTurns) > (score(g1.resetTurns) * 1.1)

  def rematch(game: Game) = game.next ?? GameRepo.game

  def feature(candidates: List[Game]) = fuccess {
    candidates sortBy { -score(_) } headOption
  }

  def score(game: Game): Int = math.round {
    (heuristics map {
      case (fn, coefficient) => heuristicBox(fn(game)) * coefficient
    }).sum * 1000
  }

  type Heuristic = Game => Float
  val heuristicBox = box(0 to 1) _
  val ratingBox = box(1000 to 2700) _
  val turnBox = box(1 to 25) _

  val heuristics: List[(Heuristic, Float)] = List(
    ratingHeuristic(Color.White) -> 1.2f,
    ratingHeuristic(Color.Black) -> 1.2f,
    progressHeuristic -> 0.7f)

  def ratingHeuristic(color: Color): Heuristic = game =>
    ratingBox(game.player(color).rating | 1400)

  def progressHeuristic: Heuristic = game =>
    1 - turnBox(game.turns)

  // boxes and reduces to 0..1 range
  def box(in: Range.Inclusive)(v: Float): Float =
    (math.max(in.start, math.min(v, in.end)) - in.start) / (in.end - in.start).toFloat
}

object ChannelActor {

  case object GetGame
  private case class SetGame(game: Game)

  case class Select(candidates: List[Game])
}
