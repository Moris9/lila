package lila.team

import lila.db.api.$find
import tube.teamTube
import lila.user.{ User, UserRepo }

private[team] final class Cli(api: TeamApi) extends lila.common.Cli {

  def process = {

    case "team" :: "join" :: team :: users  ⇒ perform(team, users)(api.doJoin)

    case "team" :: "quit" :: team :: users  ⇒ perform(team, users)(api.doQuit)

    case "team" :: "enable" :: team :: Nil  ⇒ perform(team)(api.enable)

    case "team" :: "disable" :: team :: Nil ⇒ perform(team)(api.disable)

    case "team" :: "delete" :: team :: Nil  ⇒ perform(team)(api.delete)
  }

  private def perform(teamId: String)(op: Team ⇒ Funit): Fu[String] =
    $find byId teamId flatMap {
      _.fold(fufail[String]("Team not found")) { u ⇒ op(u) inject "Success" }
    }

  private def perform(teamId: String, userIds: List[String])(op: (Team, String) ⇒ Funit): Fu[String] =
    $find byId teamId flatMap {
      _.fold(fufail[String]("Team not found")) { team ⇒
        UserRepo nameds userIds flatMap { users ⇒
          users.map(user ⇒ fuloginfo(user.username) >> op(team, user.id)).sequenceFu
        } inject "Success"
      }
    }
}
