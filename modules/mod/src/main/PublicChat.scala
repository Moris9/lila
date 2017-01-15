package lila.mod

import lila.chat.UserChat
import lila.simul.Simul
import lila.tournament.Tournament

final class PublicChat(
    chatApi: lila.chat.ChatApi,
    tournamentApi: lila.tournament.TournamentApi,
    simulEnv: lila.simul.Env) {

  def tournamentChats: Fu[List[(Tournament, UserChat)]] =
    tournamentApi.fetchVisibleTournaments.flatMap {
      visibleTournaments =>
        val ids = visibleTournaments.all.map(_.id)
        chatApi.userChat.findAll(ids).map {
          chats =>
            chats.map { chat =>
              visibleTournaments.all.find(_.id === chat.id).map(tour => (tour, chat))
            }.flatten
        } map sortTournamentsByRelevance
    }

  def simulChats: Fu[List[(Simul, UserChat)]] =
    fetchVisibleSimuls.flatMap {
      simuls =>
        val ids = simuls.map(_.id)
        chatApi.userChat.findAll(ids).map {
          chats =>
            chats.map { chat =>
              simuls.find(_.id === chat.id).map(simul => (simul, chat))
            }.flatten
        }
    }

  private def fetchVisibleSimuls: Fu[List[Simul]] = {
    simulEnv.allCreated(true) zip
      simulEnv.repo.allStarted zip
      simulEnv.repo.allFinished(3) map {
        case ((created, started), finished) =>
          created ::: started ::: finished
      }
  }

  /**
   * Sort the tournaments by the tournaments most likely to require moderation attention
   */
  private def sortTournamentsByRelevance(tournaments: List[(Tournament, UserChat)]) =
    tournaments.sortBy(-_._1.nbPlayers)
}
