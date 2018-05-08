package lila.common

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

object HttpStream {

  def onComplete(stream: Option[ActorRef], system: ActorSystem) =
    stream foreach { actor =>
      system.lilaBus.unsubscribe(actor)
      actor ! PoisonPill
    }

  case object SetOnline
}
