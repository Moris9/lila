package lila.user

import scala.math.max

final class EloUpdater(userRepo: UserRepo, historyRepo: HistoryRepo, floor: Int) {

  def game(user: User, elo: Int, opponentElo: Int): Funit = max(elo, floor) |> { newElo ⇒
    userRepo.setElo(user.id, newElo) >> historyRepo.addEntry(user.id, newElo, opponentElo.some)
  }

  private def adjustTo = Users.STARTING_ELO

  def adjust(u: User) =
    userRepo.setElo(u.id, adjustTo) >>
      historyRepo.addEntry(u.id, adjustTo, none) doIf (!u.engine && u.elo > adjustTo)
}
