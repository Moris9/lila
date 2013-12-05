package lila.setup

import akka.actor.ActorSelection
import akka.pattern.ask

import chess.{ Color ⇒ ChessColor }
import lila.game.{ GameRepo, Game, Pov, Event, Progress }
import lila.hub.actorApi.router.Player
import lila.round.Messenger
import lila.user.User
import makeTimeout.short

private[setup] final class FriendJoiner(
    messenger: Messenger,
    friendConfigMemo: FriendConfigMemo,
    timeline: ActorSelection,
    router: ActorSelection) {

  def apply(game: Game, user: Option[User]): Valid[Fu[(Pov, List[Event])]] =
    game.notStarted option {
      val color = (friendConfigMemo get game.id map (_.creatorColor)) orElse 
      // damn, no cache. maybe the opponent was logged, so we can guess?
      Some(ChessColor.Black).ifTrue(game.whitePlayer.hasUser) orElse
      Some(ChessColor.White).ifTrue(game.blackPlayer.hasUser) getOrElse
      ChessColor.Black // well no. we're fucked. toss the coin.
      for {
        p1 ← user.fold(fuccess(Progress(game))) { u ⇒
          GameRepo.setUser(game.id, color, u) inject
            Progress(game, game.updatePlayer(color, _ withUser u))
        }
        p2 = p1 map (_.start)
        url ← playerUrl(p2.game, !color)
        p3 ← messenger init p2.game map { evts ⇒
          p2 + Event.RedirectOwner(!color, url) ++ evts
        }
        _ ← (GameRepo save p3) >>
          (GameRepo denormalizeUids p3.game) >>-
          (timeline ! p3.game)
      } yield Pov(p3.game, color) -> p3.events
    } toValid "Can't join started game " + game.id

  private def playerUrl(game: Game, color: ChessColor): Fu[String] =
    router ? Player(game fullIdOf color) mapTo manifest[String]
}
