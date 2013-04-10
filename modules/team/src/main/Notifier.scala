package lila.team

import lila.hub.actorApi.message.LichessThread
import lila.hub.actorApi.router._

import akka.actor.ActorRef
import akka.pattern.ask

private[team] final class Notifier(
    messenger: ActorRef,
    router: ActorRef) {

  import makeTimeout.large

  def acceptRequest(team: Team, request: Request) {
    teamUrl(team.id) foreach { url ⇒
      messenger ! LichessThread(
        to = request.user,
        subject = """You have joined the team %s""".format(team.name),
        message = """Congratulation, your request to join the team was accepted!

Here is the team page: %s""" format url
      )
    }
  }

  private def teamUrl(id: String) =
    router ? Abs(TeamShow(id)) mapTo manifest[String] 
}
