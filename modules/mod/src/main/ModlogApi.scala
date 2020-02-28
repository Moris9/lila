package lila.mod

import lila.db.dsl._
import lila.report.{ Mod, ModId, Report, Suspect }
import lila.security.Permission
import lila.user.{ User, UserRepo }

final class ModlogApi(repo: ModlogRepo, userRepo: UserRepo, slackApi: lila.slack.SlackApi)(
    implicit ec: scala.concurrent.ExecutionContext
) {

  private def coll = repo.coll

  import lila.db.BSON.BSONJodaDateTimeHandler
  implicit private val ModlogBSONHandler = reactivemongo.api.bson.Macros.handler[Modlog]

  def streamerList(mod: Mod, streamerId: User.ID, v: Boolean) = add {
    Modlog(mod.user.id, streamerId.some, if (v) Modlog.streamerList else Modlog.streamerUnlist)
  }
  def streamerFeature(mod: Mod, streamerId: User.ID, v: Boolean) = add {
    Modlog(mod.user.id, streamerId.some, if (v) Modlog.streamerFeature else Modlog.streamerUnfeature)
  }

  def practiceConfig(mod: User.ID) = add {
    Modlog(mod, none, Modlog.practiceConfig)
  }

  def alt(mod: Mod, sus: Suspect, v: Boolean) = add {
    Modlog.make(mod, sus, if (v) Modlog.alt else Modlog.unalt)
  }

  def engine(mod: Mod, sus: Suspect, v: Boolean) = add {
    Modlog.make(mod, sus, if (v) Modlog.engine else Modlog.unengine)
  }

  def booster(mod: Mod, sus: Suspect, v: Boolean) = add {
    Modlog.make(mod, sus, if (v) Modlog.booster else Modlog.unbooster)
  }

  def troll(mod: Mod, sus: Suspect) = add {
    Modlog.make(mod, sus, if (sus.user.marks.troll) Modlog.troll else Modlog.untroll)
  }

  def ban(mod: Mod, sus: Suspect) = add {
    Modlog.make(mod, sus, if (sus.user.marks.ipban) Modlog.ipban else Modlog.ipunban)
  }

  def disableTwoFactor(mod: User.ID, user: User.ID) = add {
    Modlog(mod, user.some, Modlog.disableTwoFactor)
  }

  def closeAccount(mod: User.ID, user: User.ID) = add {
    Modlog(mod, user.some, Modlog.closeAccount)
  }

  def selfCloseAccount(user: User.ID, openReports: List[Report]) = add {
    Modlog(
      ModId.lichess.value,
      user.some,
      Modlog.selfCloseAccount,
      details = openReports.map(r => s"${r.reason.name} report").mkString(", ").some.filter(_.nonEmpty)
    )
  }

  def hasModClose(user: User.ID): Fu[Boolean] =
    coll.exists($doc("user" -> user, "action" -> Modlog.closeAccount))

  def reopenAccount(mod: User.ID, user: User.ID) = add {
    Modlog(mod, user.some, Modlog.reopenAccount)
  }

  def addTitle(mod: User.ID, user: User.ID, title: String) = add {
    Modlog(mod, user.some, Modlog.setTitle, title.some)
  }

  def removeTitle(mod: User.ID, user: User.ID) = add {
    Modlog(mod, user.some, Modlog.removeTitle)
  }

  def setEmail(mod: User.ID, user: User.ID) = add {
    Modlog(mod, user.some, Modlog.setEmail)
  }

  def ipban(mod: User.ID, ip: String) = add {
    Modlog(mod, none, Modlog.ipban, ip.some)
  }

  def deletePost(
      mod: User.ID,
      user: Option[User.ID],
      author: Option[User.ID],
      ip: Option[String],
      text: String
  ) = add {
    Modlog(
      mod,
      user,
      Modlog.deletePost,
      details = Some(
        author.??(_ + " ") + ip.??(_ + " ") + text.take(400)
      )
    )
  }

  def toggleCloseTopic(mod: User.ID, categ: String, topic: String, closed: Boolean) = add {
    Modlog(
      mod,
      none,
      if (closed) Modlog.closeTopic else Modlog.openTopic,
      details = Some(
        categ + " / " + topic
      )
    )
  }

  def toggleHideTopic(mod: User.ID, categ: String, topic: String, hidden: Boolean) = add {
    Modlog(
      mod,
      none,
      if (hidden) Modlog.hideTopic else Modlog.showTopic,
      details = Some(
        categ + " / " + topic
      )
    )
  }

  def toggleStickyTopic(mod: User.ID, categ: String, topic: String, sticky: Boolean) = add {
    Modlog(
      mod,
      none,
      if (sticky) Modlog.stickyTopic else Modlog.unstickyTopic,
      details = Some(
        categ + " / " + topic
      )
    )
  }

  def deleteTeam(mod: User.ID, name: String, desc: String) = add {
    Modlog(mod, none, Modlog.deleteTeam, details = s"$name / $desc".take(200).some)
  }

  def terminateTournament(mod: User.ID, name: String) = add {
    Modlog(mod, none, Modlog.terminateTournament, details = name.some)
  }

  def chatTimeout(mod: User.ID, user: User.ID, reason: String) = add {
    Modlog(mod, user.some, Modlog.chatTimeout, details = reason.some)
  }

  def setPermissions(mod: Mod, user: User.ID, permissions: List[Permission]) = add {
    Modlog(mod.id.value, user.some, Modlog.permissions, details = permissions.mkString(", ").some)
  }

  def reportban(mod: Mod, sus: Suspect, v: Boolean) = add {
    Modlog.make(mod, sus, if (v) Modlog.reportban else Modlog.unreportban)
  }

  def modMessage(mod: User.ID, user: User.ID, subject: String) = add {
    Modlog(mod, user.some, Modlog.modMessage, details = subject.some)
  }

  def coachReview(mod: User.ID, coach: User.ID, author: User.ID) = add {
    Modlog(mod, coach.some, Modlog.coachReview, details = s"by $author".some)
  }

  def cheatDetected(user: User.ID, gameId: String) = add {
    Modlog("lichess", user.some, Modlog.cheatDetected, details = s"game $gameId".some)
  }

  def cli(by: User.ID, command: String) = add {
    Modlog(by, none, Modlog.cli, command.some)
  }

  def garbageCollect(mod: Mod, sus: Suspect) = add {
    Modlog.make(mod, sus, Modlog.garbageCollect)
  }

  def rankban(mod: Mod, sus: Suspect, v: Boolean) = add {
    Modlog.make(mod, sus, if (v) Modlog.rankban else Modlog.unrankban)
  }

  def teamKick(mod: User.ID, user: User.ID, teamName: String) = add {
    Modlog(mod, user.some, Modlog.teamKick, details = Some(teamName take 140))
  }

  def teamEdit(mod: User.ID, teamOwner: User.ID, teamName: String) = add {
    Modlog(mod, teamOwner.some, Modlog.teamEdit, details = Some(teamName take 140))
  }

  def teamMadeOwner(mod: User.ID, user: User.ID, teamName: String) = add {
    Modlog(mod, user.some, Modlog.teamMadeOwner, details = Some(teamName take 140))
  }

  def recent = coll.ext.find($empty).sort($sort naturalDesc).cursor[Modlog]().gather[List](100)

  def wasUnengined(sus: Suspect) =
    coll.exists(
      $doc(
        "user"   -> sus.user.id,
        "action" -> Modlog.unengine
      )
    )

  def wasUnbooster(userId: User.ID) =
    coll.exists(
      $doc(
        "user"   -> userId,
        "action" -> Modlog.unbooster
      )
    )

  def userHistory(userId: User.ID): Fu[List[Modlog]] =
    coll.ext.find($doc("user" -> userId)).sort($sort desc "date").cursor[Modlog]().gather[List](30)

  private def add(m: Modlog): Funit = {
    lila.mon.mod.log.create.increment()
    lila.log("mod").info(m.toString)
    coll.insert.one(m) >>
      slackMonitor(m)
  }

  private def slackMonitor(m: Modlog): Funit = {
    import lila.mod.{ Modlog => M }
    userRepo.isMonitoredMod(m.mod) flatMap {
      _ ?? slackApi.monitorMod(
        m.mod,
        icon = m.action match {
          case M.alt | M.engine | M.booster | M.troll | M.closeAccount => "thorhammer"
          case M.unalt | M.unengine | M.unbooster | M.ipunban | M.untroll | M.reopenAccount =>
            "large_blue_circle"
          case M.deletePost | M.deleteTeam | M.terminateTournament => "x"
          case M.chatTimeout                                       => "hourglass_flowing_sand"
          case M.ban | M.ipban                                     => "ripp"
          case M.closeTopic                                        => "lock"
          case M.openTopic                                         => "unlock"
          case M.modMessage                                        => "left_speech_bubble"
          case _                                                   => "gear"
        },
        text = s"""${m.showAction.capitalize} ${m.user.??(u => s"@$u ")}${~m.details}"""
      )
    }
  }
}
