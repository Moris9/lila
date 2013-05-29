package lila.lobby

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._, lobby._
import lila.db.api._
import lila.hub.actorApi.GetUids
import lila.memo.ExpireSetMemo
import lila.socket.actorApi.Broom
import makeTimeout.short

private[lobby] final class Lobby(
    biter: Biter,
    socket: ActorRef) extends Actor {

  def receive = {

    case GetOpen       ⇒ sender ! HookRepo.allOpen
    case GetOpenCasual ⇒ sender ! HookRepo.allOpenCasual

    case msg @ AddHook(hook) ⇒ {
      HookRepo byUid hook.uid foreach remove
      HookRepo save hook
      socket ! msg
    }

    case CancelHook(uid) ⇒ {
      HookRepo byUid uid foreach remove
    }

    case BiteHook(hookId, uid, userId) ⇒ blocking {
      biter(hookId, userId) map { f ⇒
        HookRepo removeUid uid
        socket ! f(uid)
      } recover {
        case e: lila.common.LilaException ⇒
      }
    }

    case Broom ⇒ blocking {
      socket ? GetUids mapTo manifest[Iterable[String]] addEffect { uids ⇒
        (HookRepo openNotInUids uids.toSet) foreach remove
      } void
    }
  }

  private def remove(hook: Hook) = {
    HookRepo remove hook
    socket ! RemoveHook(hook.id)
  }

  private def blocking[A](f: Fu[A]): A = f await 1.second
}
