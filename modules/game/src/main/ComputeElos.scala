package lila.game

import akka.actor._
import akka.routing.RoundRobinRouter
import chess.Speed
import lila.db.api._
import lila.db.Implicits._
import lila.user.tube.userTube
import lila.user.{ User, UserRepo, SpeedElos, SpeedElo }
import play.api.libs.json.Json
import tube.gameTube

private[game] final class ComputeElos(system: ActorSystem) {

  private lazy val eloCalculator = new chess.EloCalculator(false)

  private implicit def SpeedElosZero = zero(SpeedElos.default)

  private val router = system.actorOf(Props(new Actor {
    def receive = {
      case user: User ⇒ {
        loginfo("Computing elo of " + user.id)
        apply(user).await
      }
    }
  }).withRouter(RoundRobinRouter(4)), "compute-elos-router")

  def all: Funit = $enumerate[Option[User]](usersQuery) { userOption ⇒
    userOption foreach router.!
    funit
  }

  def apply(user: User): Funit = $enumerate.fold[Option[Game], SpeedElos](gamesQuery(user)) {
    case (elos, gameOption) ⇒ (for {
      game ← gameOption
      player ← game player user
      opponentElo ← game.opponent(player).elo
    } yield {
      val speed = Speed(game.clock)
      val speedElo = elos(speed)
      val opponentSpeedElo = SpeedElo(0, opponentElo)
      val (white, black) = player.color.fold[(eloCalculator.User, eloCalculator.User)](
        speedElo -> opponentSpeedElo,
        opponentSpeedElo -> speedElo)
      val newElos = eloCalculator.calculate(white, black, game.winnerColor)
      val newElo = player.color.fold(newElos._1, newElos._2)
      elos.addGame(speed, newElo)
    }) | elos
  } flatMap UserRepo.setSpeedElos(user.id)

  private def usersQuery = $query.apply[User](
    Json.obj(
      "count.rated" -> $gt(0),
      "speedElos" -> $exists(false)
    )) sort ($sort desc "seenAt")

  private def gamesQuery(user: User) = $query.apply[Game](
    Query.finished ++ Query.rated ++ Query.user(user.id)
  ) sort ($sort asc Game.ShortFields.createdAt)

}
