package lila.round

import lila.chat.{ Chat, ChatApi, ChatTimeout }
import lila.game.Game
import lila.hub.actorApi.shutup.PublicSource
import lila.user.User

final class Messenger(api: ChatApi) {

  private[round] val busChan = "chat:round"

  def system(game: Game, message: String): Unit =
    system(true)(game, message)

  def volatile(game: Game, message: String): Unit =
    system(false)(game, message)

  def system(persistent: Boolean)(game: Game, message: String): Unit = {
    val apiCall =
      if (persistent) api.userChat.system _
      else api.userChat.volatile _
    apiCall(watcherId(Chat.Id(game.id)), message, busChan.some)
    if (game.nonAi) apiCall(Chat.Id(game.id), message, busChan.some)
  }

  def systemForOwners(chatId: Chat.Id, message: String): Unit = {
    api.userChat.system(chatId, message, busChan.some)
  }

  def watcher(gameId: Game.Id, userId: User.ID, text: String) =
    api.userChat.write(watcherId(gameId), userId, text, PublicSource.Watcher(gameId.value).some, busChan.some)

  private val whisperCommands = List("/whisper ", "/w ")

  def owner(gameId: Game.Id, userId: User.ID, text: String): Unit =
    whisperCommands.collectFirst {
      case command if text startsWith command =>
        val source = PublicSource.Watcher(gameId.value)
        api.userChat.write(watcherId(gameId), userId, text drop command.size, source.some, busChan.some)
    } getOrElse {
      if (!text.startsWith("/")) // mistyped command?
        api.userChat.write(Chat.Id(gameId.value), userId, text, publicSource = none, busChan.some).some
    }

  def owner(gameId: Game.Id, anonColor: chess.Color, text: String): Unit =
    api.playerChat.write(Chat.Id(gameId.value), anonColor, text, busChan)

  def timeout(chatId: Chat.Id, modId: User.ID, suspect: User.ID, reason: String, text: String): Unit =
    ChatTimeout.Reason(reason) foreach { r =>
      api.userChat.timeout(chatId, modId, suspect, r, ChatTimeout.Scope.Global, text)
    }

  private def watcherId(chatId: Chat.Id) = Chat.Id(s"$chatId/w")
  private def watcherId(gameId: Game.Id) = Chat.Id(s"$gameId/w")
}
