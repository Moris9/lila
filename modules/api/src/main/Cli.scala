package lila.api

import akka.actor.ActorSelection
import akka.pattern.{ ask, pipe }
import play.api.templates.Html

import lila.hub.actorApi.{ RemindDeploy, Deploy }
import makeTimeout.short

private[api] final class Cli(bus: lila.common.Bus, renderer: ActorSelection) extends lila.common.Cli {

  def apply(args: List[String]): Fu[String] = run(args).map(_ + "\n") ~ {
    _ logFailure ("[cli] " + args.mkString(" ")) foreach { output ⇒
      loginfo("[cli] %s\n%s".format(args mkString " ", output))
    }
  }

  def process = {
    case "deploy" :: "pre" :: Nil  ⇒ remindDeploy(lila.hub.actorApi.RemindDeployPre)
    case "deploy" :: "post" :: Nil ⇒ remindDeploy(lila.hub.actorApi.RemindDeployPost)
    case "glicko" :: "migration" :: Nil => GlickoMigration(
      lila.db.Env.current,
      lila.game.Env.current,
      lila.user.Env.current)
    case "glicko" :: "migration" :: "end" :: Nil => GlickoMigrationEnd(
      lila.db.Env.current,
      lila.game.Env.current,
      lila.user.Env.current)
    case "glicko" :: "migration" :: "fix" :: Nil => GlickoMigrationFix(
      lila.db.Env.current,
      lila.game.Env.current,
      lila.user.Env.current)
  }

  private def remindDeploy(event: RemindDeploy): Fu[String] = {
    renderer ? event foreach {
      case html: Html ⇒ bus.publish(Deploy(event, html.body), 'deploy)
    }
    fuccess("Deploy in progress")
  }

  private def run(args: List[String]): Fu[String] = {
    (processors lift args) | fufail("Unknown command: " + args.mkString(" "))
  } recover {
    case e: Exception ⇒ "ERROR " + e
  }

  private def processors =
    lila.user.Env.current.cli.process orElse
      lila.security.Env.current.cli.process orElse
      lila.wiki.Env.current.cli.process orElse
      lila.i18n.Env.current.cli.process orElse
      lila.game.Env.current.cli.process orElse
      lila.gameSearch.Env.current.cli.process orElse
      lila.teamSearch.Env.current.cli.process orElse
      lila.forum.Env.current.cli.process orElse
      lila.forumSearch.Env.current.cli.process orElse
      lila.message.Env.current.cli.process orElse
      lila.tournament.Env.current.cli.process orElse
      lila.analyse.Env.current.cli.process orElse
      lila.team.Env.current.cli.process orElse
      process
}
