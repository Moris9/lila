package lila.chat
package actorApi

case class UserTalk(chatId: Chat.Id, userId: String, text: String, public: Boolean = true)
case class PlayerTalk(chatId: Chat.Id, white: Boolean, text: String)
case class SystemTalk(chatId: Chat.Id, text: String)
case class ChatLine(chatId: Chat.Id, line: Line)
case class Timeout(chatId: Chat.Id, mod: String, userId: String, reason: ChatTimeout.Reason, local: Boolean)

case class OnTimeout(username: String)
case class OnReinstate(userId: String)
case class Remove(chatId: Chat.Id)
