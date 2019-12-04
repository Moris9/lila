package lila.api

import akka.actor._
import akka.stream.scaladsl._
import play.api.libs.json._
import scala.concurrent.duration._

import lila.challenge.{ Challenge, ChallengeMaker }
import lila.common.Bus
import lila.game.actorApi.UserStartGame
import lila.game.Game
import lila.hub.actorApi.socket.BotIsOnline
import lila.user.User

final class EventStream(
    challengeJsonView: lila.challenge.JsonView,
    challengeMaker: lila.challenge.ChallengeMaker,
    onlineBots: lila.bot.OnlineBots
)(implicit system: ActorSystem) {

  private case object SetOnline

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def plugTo(me: User, gamesInProgress: List[Game], challenges: List[Challenge])(
    sink: Sink[Option[JsObject], Fu[akka.Done]]
  ): Unit = {

    val (queue, done) = blueprint.toMat(sink)(Keep.both).run()

    gamesInProgress map toJson map some foreach queue.offer
    challenges map toJson map some foreach queue.offer

    val actor = system.actorOf(Props(mkActor(me, queue)))

    done onComplete { _ => actor ! PoisonPill }
  }

  private def mkActor(me: User, queue: SourceQueueWithComplete[Option[JsObject]]) = new Actor {

    val classifiers = List(
      s"userStartGame:${me.id}",
      s"rematchFor:${me.id}",
      "challenge"
    )

    override def preStart(): Unit = {
      super.preStart()
      Bus.subscribe(self, classifiers)
    }

    override def postStop() = {
      super.postStop()
      Bus.unsubscribe(self, classifiers)
    }

    self ! SetOnline

    def receive = {

      case SetOnline =>
        onlineBots.setOnline(me.id)
        context.system.scheduler.scheduleOnce(6 second) {
          // gotta send a message to check if the client has disconnected
          queue offer None
          self ! SetOnline
        }

      case UserStartGame(userId, game) if userId == me.id => queue offer toJson(game).some

      case lila.challenge.Event.Create(c) if c.destUserId has me.id => queue offer toJson(c).some

      // pretend like the rematch is a challenge
      case lila.hub.actorApi.round.RematchOffer(gameId) => challengeMaker.makeRematchFor(gameId, me) foreach {
        _ foreach { c =>
          queue offer toJson(c.copy(_id = gameId)).some
        }
      }
    }
  }

  private def toJson(game: Game) = Json.obj(
    "type" -> "gameStart",
    "game" -> Json.obj("id" -> game.id)
  )
  private def toJson(c: Challenge) = Json.obj(
    "type" -> "challenge",
    "challenge" -> challengeJsonView(none)(c)
  )
}
