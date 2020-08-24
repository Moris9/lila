package lila.api

import akka.actor._
import akka.stream.scaladsl._
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.duration._

import lila.challenge.Challenge
import lila.common.Bus
import lila.game.actorApi.{ FinishGame, StartGame }
import lila.game.Game
import lila.user.{ User, UserRepo }

final class EventStream(
    challengeJsonView: lila.challenge.JsonView,
    challengeMaker: lila.challenge.ChallengeMaker,
    onlineApiUsers: lila.bot.OnlineApiUsers,
    userRepo: UserRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private case object SetOnline

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(
      me: User,
      gamesInProgress: List[Game],
      challenges: List[Challenge]
  ): Source[Option[JsObject], _] = {

    // kill previous one if any
    Bus.publish(PoisonPill, s"eventStreamFor:${me.id}")

    blueprint mapMaterializedValue { queue =>
      gamesInProgress map gameJson("gameStart") map some foreach queue.offer
      challenges map challengeJson("challenge") map some foreach queue.offer

      val actor = system.actorOf(Props(mkActor(me, queue)))

      queue.watchCompletion().foreach { _ =>
        actor ! PoisonPill
      }
    }
  }

  private def mkActor(me: User, queue: SourceQueueWithComplete[Option[JsObject]]) =
    new Actor {

      val classifiers = List(
        s"userStartGame:${me.id}",
        s"userFinishGame:${me.id}",
        s"rematchFor:${me.id}",
        s"eventStreamFor:${me.id}",
        "challenge"
      )

      var lastSetSeenAt = me.seenAt | me.createdAt
      var online        = true

      override def preStart(): Unit = {
        super.preStart()
        Bus.subscribe(self, classifiers)
      }

      override def postStop() = {
        super.postStop()
        Bus.unsubscribe(self, classifiers)
        queue.complete()
        online = false
      }

      self ! SetOnline

      def receive = {

        case SetOnline =>
          onlineApiUsers.setOnline(me.id)

          if (lastSetSeenAt isBefore DateTime.now.minusMinutes(2)) {
            userRepo setSeenAt me.id
            lastSetSeenAt = DateTime.now
          }

          context.system.scheduler.scheduleOnce(6 second) {
            if (online) {
              // gotta send a message to check if the client has disconnected
              queue offer None
              self ! SetOnline
            }
          }

        case StartGame(game) => queue offer gameJson("gameStart")(game).some

        case FinishGame(game, _, _) => queue offer gameJson("gameFinish")(game).some

        case lila.challenge.Event.Create(c) if c.destUserId has me.id =>
          queue offer challengeJson("challenge")(c).some

        case lila.challenge.Event.Decline(c) if c.challengerUserId has me.id =>
          queue offer challengeJson("challengeDeclined")(c).some

        case lila.challenge.Event.Cancel(c) if c.destUserId has me.id =>
          queue offer challengeJson("challengeCanceled")(c).some

        // pretend like the rematch is a challenge
        case lila.hub.actorApi.round.RematchOffer(gameId) =>
          challengeMaker.makeRematchFor(gameId, me) foreach {
            _ foreach { c =>
              queue offer challengeJson("challenge")(c.copy(_id = gameId)).some
            }
          }
      }
    }

  private def gameJson(tpe: String)(game: Game) =
    Json.obj(
      "type" -> tpe,
      "game" -> Json.obj("id" -> game.id)
    )

  private def challengeJson(tpe: String)(c: lila.challenge.Challenge) =
    Json.obj(
      "type"      -> tpe,
      "challenge" -> challengeJsonView(none)(c)(lila.i18n.defaultLang)
    )
}
