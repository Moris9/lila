package lila.hub

import akka.actor._
import com.typesafe.config.Config

final class Env(config: Config, system: ActorSystem) {

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
    val simul = select("actor.simul")
    val timeline = select("actor.timeline.user")
    val bookmark = select("actor.bookmark")
    val roundMap = select("actor.round.map")
    val lobby = select("actor.lobby")
    val relation = select("actor.relation")
    val challenger = select("actor.challenger")
    val report = select("actor.report")
    val shutup = select("actor.shutup")
    val mod = select("actor.mod")
    val chat = select("actor.chat")
    val analyser = select("actor.analyser")
    val moveBroadcast = select("actor.move_broadcast")
    val userRegister = select("actor.user_register")
  }

  object socket {
    val lobby = select("socket.lobby")
    val round = select("socket.round")
    val tournament = select("socket.tournament")
    val simul = select("socket.simul")
    val site = select("socket.site")
    val monitor = select("socket.monitor")
    val hub = select("socket.hub")
  }

  private def select(name: String) =
    system actorSelection ("/user/" + config.getString(name))
}

object Env {

  lazy val current = "hub" boot new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = lila.common.PlayApp.system)
}
