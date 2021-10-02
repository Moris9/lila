package lila.chat

import chess.Color
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.config.NetDomain
import lila.common.String.{ fullCleanUp, noShouting }
import lila.db.dsl._
import lila.hub.actorApi.shutup.{ PublicSource, RecordPrivateChat, RecordPublicChat }
import lila.memo.CacheApi._
import lila.user.{ Holder, User, UserRepo }

final class ChatApi(
    coll: Coll,
    userRepo: UserRepo,
    chatTimeout: ChatTimeout,
    flood: lila.security.Flood,
    spam: lila.security.Spam,
    shutup: lila.hub.actors.Shutup,
    cacheApi: lila.memo.CacheApi,
    netDomain: NetDomain
)(implicit ec: scala.concurrent.ExecutionContext, actorSystem: akka.actor.ActorSystem) {

  import Chat.{ chatIdBSONHandler, userChatBSONHandler }

  def exists(id: String) = coll.exists($id(id))

  object userChat {

    // only use for public, multi-user chats - tournaments, simuls
    object cached {

      private val cache = cacheApi[Chat.Id, UserChat](1024, "chat.user") {
        _.expireAfterWrite(1 minute).buildAsyncFuture(find)
      }

      def invalidate = cache.invalidate _

      def findMine(chatId: Chat.Id, me: Option[User]): Fu[UserChat.Mine] =
        me match {
          case Some(user) => findMine(chatId, user)
          case None       => cache.get(chatId) dmap { UserChat.Mine(_, timeout = false) }
        }

      private def findMine(chatId: Chat.Id, me: User): Fu[UserChat.Mine] =
        cache get chatId flatMap { chat =>
          (!chat.isEmpty ?? chatTimeout.isActive(chatId, me.id)) dmap {
            UserChat.Mine(chat forUser me.some, _)
          }
        }
    }

    def findOption(chatId: Chat.Id): Fu[Option[UserChat]] =
      coll.byId[UserChat](chatId.value)

    def find(chatId: Chat.Id): Fu[UserChat] =
      findOption(chatId) dmap (_ | Chat.makeUser(chatId))

    def findAll(chatIds: List[Chat.Id]): Fu[List[UserChat]] =
      coll.byIds[UserChat](chatIds.map(_.value), ReadPreference.secondaryPreferred)

    def findMine(chatId: Chat.Id, me: Option[User]): Fu[UserChat.Mine] = findMineIf(chatId, me, cond = true)

    def findMineIf(chatId: Chat.Id, me: Option[User], cond: Boolean): Fu[UserChat.Mine] =
      me match {
        case Some(user) if cond => findMine(chatId, user)
        case Some(user)         => fuccess(UserChat.Mine(Chat.makeUser(chatId) forUser user.some, timeout = false))
        case None if cond       => find(chatId) dmap { UserChat.Mine(_, timeout = false) }
        case None               => fuccess(UserChat.Mine(Chat.makeUser(chatId), timeout = false))
      }

    private def findMine(chatId: Chat.Id, me: User): Fu[UserChat.Mine] =
      find(chatId) flatMap { chat =>
        (!chat.isEmpty ?? chatTimeout.isActive(chatId, me.id)) dmap {
          UserChat.Mine(chat forUser me.some, _)
        }
      }

    def write(
        chatId: Chat.Id,
        userId: User.ID,
        text: String,
        publicSource: Option[PublicSource],
        busChan: BusChan.Select,
        persist: Boolean = true
    ): Funit =
      makeLine(chatId, userId, text) flatMap {
        _ ?? { line =>
          linkCheck(line, publicSource) flatMap {
            case false =>
              logger.info(s"Link check rejected $line in $publicSource")
              funit
            case true =>
              (persist ?? persistLine(chatId, line, expire = publicSource.isEmpty)) >>- {
                if (persist) {
                  if (publicSource.isDefined) cached invalidate chatId
                  shutup ! {
                    publicSource match {
                      case Some(source) => RecordPublicChat(userId, text, source)
                      case _            => RecordPrivateChat(chatId.value, userId, text)
                    }
                  }
                  lila.mon.chat
                    .message(publicSource.fold("player")(_.parentName), line.troll)
                    .increment()
                    .unit
                }
                publish(chatId, actorApi.ChatLine(chatId, line), busChan)
              }
          }
        }
      }

    private def linkCheck(line: UserLine, source: Option[PublicSource]) =
      source.fold(fuccess(true)) { s =>
        Bus.ask[Boolean]("chatLinkCheck") { GetLinkCheck(line, s, _) }
      }

    def clear(chatId: Chat.Id) = coll.delete.one($id(chatId)).void

    def system(chatId: Chat.Id, text: String, busChan: BusChan.Select, expire: Boolean): Funit = {
      val line = UserLine(systemUserId, None, false, text, troll = false, deleted = false)
      persistLine(chatId, line, expire) >>- {
        cached.invalidate(chatId)
        publish(chatId, actorApi.ChatLine(chatId, line), busChan)
      }
    }

    // like system, but not persisted.
    def volatile(chatId: Chat.Id, text: String, busChan: BusChan.Select): Unit = {
      val line = UserLine(systemUserId, None, false, text, troll = false, deleted = false)
      publish(chatId, actorApi.ChatLine(chatId, line), busChan)
    }

    def service(
        chatId: Chat.Id,
        text: String,
        busChan: BusChan.Select,
        isVolatile: Boolean,
        expire: Boolean
    ): Unit = {
      if (isVolatile) volatile(chatId, text, busChan) else system(chatId, text, busChan, expire)
    }.unit

    def timeout(
        chatId: Chat.Id,
        modId: User.ID,
        userId: User.ID,
        reason: ChatTimeout.Reason,
        scope: ChatTimeout.Scope,
        text: String,
        busChan: BusChan.Select
    ): Funit =
      coll.byId[UserChat](chatId.value) zip userRepo.byId(modId) zip userRepo.byId(userId) flatMap {
        case ((Some(chat), Some(mod)), Some(user)) if isMod(mod) || scope == ChatTimeout.Scope.Local =>
          doTimeout(chat, mod, user, reason, scope, text, busChan)
        case _ => funit
      }

    def publicTimeout(data: ChatTimeout.TimeoutFormData, me: Holder): Funit =
      ChatTimeout.Reason(data.reason) ?? { reason =>
        timeout(
          chatId = Chat.Id(data.roomId),
          modId = me.id,
          userId = data.userId,
          reason = reason,
          scope = ChatTimeout.Scope.Global,
          text = data.text,
          busChan = if (data.chan == "tournament") _.Tournament else _.Simul
        )
      }

    def userModInfo(username: String): Fu[Option[UserModInfo]] =
      userRepo named username flatMap {
        _ ?? { user =>
          chatTimeout.history(user, 20) dmap { UserModInfo(user, _).some }
        }
      }

    private def doTimeout(
        c: UserChat,
        mod: User,
        user: User,
        reason: ChatTimeout.Reason,
        scope: ChatTimeout.Scope,
        text: String,
        busChan: BusChan.Select
    ): Funit =
      chatTimeout.add(c, mod, user, reason, scope) flatMap {
        _ ?? {
          val lineText = scope match {
            case ChatTimeout.Scope.Global => s"${user.username} was timed out 15 minutes for ${reason.name}."
            case _                        => s"${user.username} was timed out 15 minutes by a page mod (not a Lichess mod)"
          }
          val line = c.hasRecentLine(user) option UserLine(
            username = systemUserId,
            title = None,
            patron = user.isPatron,
            text = lineText,
            troll = false,
            deleted = false
          )
          val c2   = c.markDeleted(user)
          val chat = line.fold(c2)(c2.add)
          coll.update.one($id(chat.id), chat).void >>- {
            cached.invalidate(chat.id)
            publish(chat.id, actorApi.OnTimeout(chat.id, user.id), busChan)
            line foreach { l =>
              publish(chat.id, actorApi.ChatLine(chat.id, l), busChan)
            }
            if (isMod(mod)) {
              lila.common.Bus.publish(
                lila.hub.actorApi.mod.ChatTimeout(
                  mod = mod.id,
                  user = user.id,
                  reason = reason.key,
                  text = text
                ),
                "chatTimeout"
              )
              lila.common.Bus
                .publish(lila.hub.actorApi.security.DeletePublicChats(user.id), "deletePublicChats")
            } else logger.info(s"${mod.username} times out ${user.username} in #${c.id} for ${reason.key}")
          }
        }
      }

    def delete(c: UserChat, user: User, busChan: BusChan.Select): Fu[Boolean] = {
      val chat   = c.markDeleted(user)
      val change = chat != c
      change.?? {
        coll.update.one($id(chat.id), chat).void >>- {
          cached invalidate chat.id
          publish(chat.id, actorApi.OnTimeout(chat.id, user.id), busChan)
        }
      } inject change
    }

    private def isMod(user: User) = lila.security.Granter(_.ChatTimeout)(user)

    def reinstate(list: List[ChatTimeout.Reinstate]) =
      list.foreach { r =>
        Bus.publish(actorApi.OnReinstate(Chat.Id(r.chat), r.user), BusChan.Global.chan)
      }

    private[ChatApi] def makeLine(chatId: Chat.Id, userId: String, t1: String): Fu[Option[UserLine]] =
      userRepo.speaker(userId) zip chatTimeout.isActive(chatId, userId) dmap {
        case (Some(user), false) if user.enabled =>
          Writer.preprocessUserInput(t1, user.username.some) flatMap { t2 =>
            val allow =
              if (user.isBot) !lila.common.String.hasLinks(t2)
              else flood.allowMessage(userId, t2)
            allow option {
              UserLine(
                user.username,
                user.title,
                user.isPatron,
                t2,
                troll = user.isTroll,
                deleted = false
              )
            }
          }
        case _ => none
      }
  }

  object playerChat {

    def findOption(chatId: Chat.Id): Fu[Option[MixedChat]] =
      coll.byId[MixedChat](chatId.value)

    def find(chatId: Chat.Id): Fu[MixedChat] =
      findOption(chatId) dmap (_ | Chat.makeMixed(chatId))

    def findIf(chatId: Chat.Id, cond: Boolean): Fu[MixedChat] =
      if (cond) find(chatId)
      else fuccess(Chat.makeMixed(chatId))

    def findNonEmpty(chatId: Chat.Id): Fu[Option[MixedChat]] =
      findOption(chatId) dmap (_ filter (_.nonEmpty))

    def optionsByOrderedIds(chatIds: List[Chat.Id]): Fu[List[Option[MixedChat]]] =
      coll.optionsByOrderedIds[MixedChat, Chat.Id](chatIds, none, ReadPreference.secondaryPreferred)(_.id)

    def write(chatId: Chat.Id, color: Color, text: String, busChan: BusChan.Select): Funit =
      makeLine(chatId, color, text) ?? { line =>
        persistLine(chatId, line, expire = true) >>- {
          publish(chatId, actorApi.ChatLine(chatId, line), busChan)
          lila.mon.chat.message("anonPlayer", troll = false).increment().unit
        }
      }

    private def makeLine(chatId: Chat.Id, color: Color, t1: String): Option[Line] =
      Writer.preprocessUserInput(t1, none) flatMap { t2 =>
        flood.allowMessage(s"$chatId/${color.letter}", t2) option
          PlayerLine(color, t2)
      }
  }

  private def publish(chatId: Chat.Id, msg: Any, busChan: BusChan.Select): Unit = {
    Bus.publish(msg, busChan(BusChan).chan)
    Bus.publish(msg, Chat chanOf chatId)
  }

  def remove(chatId: Chat.Id) = coll.delete.one($id(chatId)).void

  def removeAll(chatIds: List[Chat.Id]) = coll.delete.one($inIds(chatIds)).void

  private def persistLine(chatId: Chat.Id, line: Line, expire: Boolean): Funit =
    coll.update
      .one(
        $id(chatId),
        $doc(
          "$push" -> $doc(
            Chat.BSONFields.lines -> $doc(
              "$each"  -> List(line),
              "$slice" -> -200
            )
          )
        ),
        upsert = true
      ) map { res =>
      (expire && res.upserted.nonEmpty) ?? coll.updateFieldUnchecked(
        $id(chatId),
        Chat.BSONFields.expire,
        DateTime.now plusMonths 6
      )
    }

  private object Writer {

    import java.util.regex.{ Matcher, Pattern }

    def preprocessUserInput(in: String, username: Option[User.ID]): Option[String] = {
      val out1 = multiline(
        spam.replace(noShouting(noPrivateUrl(fullCleanUp(in))))
      )
      val out2 = username.fold(out1) { removeSelfMention(out1, _) }
      out2.take(Line.textMaxSize).some.filter(_.nonEmpty)
    }

    private def removeSelfMention(in: String, username: User.ID) =
      if (in.contains('@'))
        ("""(?i)@(?<![\w@#/]@)""" + username + """(?![@\w-]|\.\w)""").r.replaceAllIn(in, username)
      else in

    private val gameUrlRegex   = (Pattern.quote(netDomain.value) + """\b/(\w{8})\w{4}\b""").r
    private val gameUrlReplace = Matcher.quoteReplacement(netDomain.value) + "/$1"

    private def noPrivateUrl(str: String): String = gameUrlRegex.replaceAllIn(str, gameUrlReplace)
    private val multilineRegex                    = """\n\n{1,}+""".r
    private def multiline(str: String)            = multilineRegex.replaceAllIn(str, " ")
  }
}
