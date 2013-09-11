package lila.mod

import lila.db.api._
import lila.security.{ Firewall, UserSpy, Store ⇒ SecurityStore }
import lila.user.tube.userTube
import lila.user.{ User, UserRepo, EloUpdater }

final class ModApi(
    logApi: ModlogApi,
    userSpy: String ⇒ Fu[UserSpy],
    firewall: Firewall,
    eloUpdater: EloUpdater,
    lobbySocket: lila.hub.ActorLazyRef) {

  def adjust(mod: String, username: String): Funit = withUser(username) { user ⇒
    logApi.engine(mod, user.id, !user.engine) zip
      UserRepo.toggleEngine(user.id) zip
      eloUpdater.adjust(user) void
  }

  def troll(mod: String, username: String): Funit = withUser(username) { u ⇒
    val user = u.copy(troll = !u.troll)
    (UserRepo updateTroll user) >>-
      logApi.troll(mod, user.id, user.troll) void
  }

  def ban(mod: String, username: String): Funit = withUser(username) { user ⇒
    userSpy(user.id) flatMap { spy ⇒
      UserRepo.toggleIpBan(user.id) zip
        logApi.ban(mod, user.id, !user.ipBan) zip
        user.ipBan.fold(
          (spy.ipStrings map firewall.unblockIp).sequenceFu,
          (spy.ipStrings map firewall.blockIp).sequenceFu >>
            (SecurityStore disconnect user.id)
        ) void
    }
  }

  def reopenAccount(mod: String, username: String): Funit = withUser(username) { user ⇒
    !user.enabled ?? {
      (UserRepo enable user.id) >> logApi.reopenAccount(mod, user.id) 
    }
  }

  def ipban(mod: String, ip: String): Funit =
    (firewall blockIp ip) >> logApi.ipban(mod, ip)

  private def withUser[A](username: String)(op: User ⇒ Fu[A]): Fu[A] =
    UserRepo named username flatten "[mod] missing user " + username flatMap op
}
