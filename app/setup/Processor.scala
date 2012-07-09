package lila
package setup

import http.Context
import game.{ DbGame, GameRepo, Pov }
import user.User
import chess.{ Game, Board, Color => ChessColor }
import ai.Ai
import lobby.{ Hook, Fisherman }
import i18n.I18nDomain
import controllers.routes

import scalaz.effects._

final class Processor(
    configRepo: UserConfigRepo,
    friendConfigMemo: FriendConfigMemo,
    gameRepo: GameRepo,
    fisherman: Fisherman,
    timelinePush: DbGame ⇒ IO[Unit],
    ai: () ⇒ Ai) extends core.Futuristic {

  def ai(config: AiConfig)(implicit ctx: Context): IO[Pov] = for {
    _ ← ctx.me.fold(
      user ⇒ configRepo.update(user)(_ withAi config),
      io()
    )
    pov = config.pov
    game = ctx.me.fold(
      user ⇒ pov.game.updatePlayer(pov.color, _ withUser user),
      pov.game)
    _ ← gameRepo insert game
    _ ← gameRepo denormalizeStarted game
    _ ← timelinePush(game)
    pov2 ← game.player.isHuman.fold(
      io(pov),
      for {
        initialFen ← game.variant.standard.fold(
          io(none[String]),
          gameRepo initialFen game.id)
        aiResult ← { ai().play(game, initialFen) map (_.err) }.toIo
        (newChessGame, move) = aiResult
        progress = game.update(newChessGame, move)
        _ ← gameRepo save progress
      } yield pov withGame progress.game
    )
  } yield pov2

  def friend(config: FriendConfig)(implicit ctx: Context): IO[Pov] = for {
    _ ← ctx.me.fold(
      user ⇒ configRepo.update(user)(_ withFriend config),
      io()
    )
    pov = config.pov
    game = ctx.me.fold(
      user ⇒ pov.game.updatePlayer(pov.color, _ withUser user),
      pov.game)
    _ ← gameRepo insert game
    _ ← timelinePush(game)
    _ ← friendConfigMemo.set(pov.game.id, config)
  } yield pov

  def hook(config: HookConfig)(implicit ctx: Context): IO[Hook] = for {
    _ ← ctx.me.fold(
      user ⇒ configRepo.update(user)(_ withHook config),
      io()
    )
    hook = config hook ctx.me
    _ ← fisherman add hook
  } yield hook

  def api(implicit ctx: Context): IO[Map[String, Any]] = {
    val domain = "http://" + I18nDomain(ctx.req.domain).commonDomain
    val config = ApiConfig
    val pov = config.pov
    val game = ctx.me.fold(
      user ⇒ pov.game.updatePlayer(pov.color, _ withUser user),
      pov.game).start
    for {
      _ ← gameRepo insert game
      _ ← timelinePush(game)
    } yield ChessColor.all map { color ⇒
      color.name -> (domain + routes.Round.player(game fullIdOf color).url)
    } toMap
  }
}
