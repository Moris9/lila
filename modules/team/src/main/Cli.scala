package lila.team

import lila.db.dsl._

final private[team] class Cli(
    userRepo: lila.user.UserRepo,
    teamRepo: TeamRepo,
    api: TeamApi
) extends lila.common.Cli {

  import BSONHandlers._

  def process = {

    case "team" :: "quit" :: team :: users => perform(team, users)(api.doQuit)

    case "team" :: "enable" :: team :: Nil => perform(team)(api.enable)

    case "team" :: "disable" :: team :: Nil => perform(team)(api.disable)

    case "team" :: "recompute" :: "nbMembers" :: Nil =>
      api.recomputeNbMembers inject "done"
  }

  private def perform(teamId: String)(op: Team => Funit): Fu[String] =
    teamRepo.coll.byId[Team](teamId) flatMap {
      _.fold(fufail[String]("Team not found")) { u =>
        op(u) inject "Success"
      }
    }

  private def perform(teamId: String, userIds: List[String])(op: (Team, String) => Funit): Fu[String] =
    teamRepo.coll.byId[Team](teamId) flatMap {
      _.fold(fufail[String]("Team not found")) { team =>
        userRepo nameds userIds flatMap { users =>
          users
            .map(user => {
              logger.info(user.username)
              op(team, user.id)
            })
            .sequenceFu
        } inject "Success"
      }
    }
}
