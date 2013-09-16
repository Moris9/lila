package lila.lobby

import actorApi.{ RemoveHook, BiteHook, JoinHook }
import akka.actor.ActorRef
import chess.{ Game ⇒ ChessGame, Board, Variant, Mode, Clock, Color ⇒ ChessColor }
import lila.game.{ GameRepo, Game, Player, Pov, Progress }
import lila.user.{ User, UserRepo }

private[lobby] final class Biter(
    timeline: lila.hub.ActorLazyRef,
    blocks: (String, String) ⇒ Fu[Boolean],
    roundMessenger: lila.round.Messenger) {

  def apply(hook: Hook, userId: Option[String]): Fu[String ⇒ JoinHook] =
    userId ?? UserRepo.byId flatMap { user ⇒
      canJoin(hook, user).fold(join(hook, user), fufail("Cannot join"))
    }

  private def join(hook: Hook, userOption: Option[User]): Fu[String ⇒ JoinHook] = for {
    ownerOption ← hook.userId ?? UserRepo.byId
    game = blame(
      _.invitedColor, userOption,
      blame(_.creatorColor, ownerOption, makeGame(hook))
    ).start
    _ ← (GameRepo insertDenormalized game) >>-
      (timeline ! game) >>
      // messenges are not sent to the game socket
      // as nobody is there to see them yet
      (roundMessenger init game)
  } yield uid ⇒ JoinHook(uid, hook, game)

  def blame(color: Game ⇒ ChessColor, userOption: Option[User], game: Game) =
    userOption.fold(game)(user ⇒ game.updatePlayer(color(game), _ withUser user))

  private def makeGame(hook: Hook) = Game.make(
    game = ChessGame(
      board = Board init hook.realVariant,
      clock = hook.hasClock.fold(
        hook.time |@| hook.increment apply { (limit, inc) ⇒
          Clock(limit = limit, increment = inc)
        },
        none)
    ),
    whitePlayer = Player.white,
    blackPlayer = Player.black,
    creatorColor = hook.realColor.resolve,
    mode = hook.realMode,
    variant = hook.realVariant,
    source = lila.game.Source.Lobby,
    pgnImport = None)

  def canJoin(hook: Hook, userOption: Option[User]) =
    hook.open &&
      hook.realMode.casual.fold(
        userOption.isDefined || hook.allowAnon,
        userOption ?? { u ⇒ hook.realEloRange.fold(true)(_ contains u.elo) }
      ) && !{
          userOption ?? { u ⇒
            hook.userId ?? { blocks(_, u.id).await }
          }
        }
}
