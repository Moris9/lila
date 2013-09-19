package lila.hub

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val SocketHubName = config getString "socket.hub.name"
  private val SocketHubTimeout = config duration "socket.hub.timeout"

  object actor {
    val game = select("actor.game.actor")
    val gameIndexer = select("actor.game.indexer")
    val renderer = select("actor.renderer")
    val captcher = select("actor.captcher")
    val forum = select("actor.forum.actor")
    val forumIndexer = select("actor.forum.indexer")
    val messenger = select("actor.messenger")
    val router = select("actor.router")
    val teamIndexer = select("actor.team.indexer")
    val ai = select("actor.ai")
    val monitor = select("actor.monitor")
    val tournamentOrganizer = select("actor.tournament.organizer")
    val gameTimeline = select("actor.timeline.game")
    val timeline = select("actor.timeline.user")
    val bookmark = select("actor.bookmark")
    val roundMap = select("actor.round.map")
    val round = select("actor.round.actor")
    val lobby = select("actor.lobby")
    val relation = select("actor.relation")
    val challenger = select("actor.challenger")
  }

  object socket {
    val lobby = select("socket.lobby")
    val monitor = select("socket.monitor")
    val site = select("socket.site")
    val round = select("socket.round")
    val tournament = select("socket.tournament")
    val hub = select(SocketHubName)
  }

  system.actorOf(Props(new Broadcast(List(
    socket.lobby,
    socket.site,
    socket.round,
    socket.tournament
  ))(makeTimeout(SocketHubTimeout))), name = SocketHubName)

  private def select(name: String) = 
    system actorSelection ("/user/" + config.getString(name))
}

object Env {

  lazy val current = "[boot] hub" describes new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
