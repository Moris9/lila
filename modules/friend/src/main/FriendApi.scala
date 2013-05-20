package lila.friend

import lila.game.GameRepo
import lila.user.{ User, UserRepo }
import lila.user.tube.userTube
import tube.{ friendTube, requestTube }
import lila.db.api._
import lila.db.Implicits._

import org.scala_tools.time.Imports._

final class FriendApi(cached: Cached) {

  def areFriends(u1: ID, u2: ID): Fu[Boolean] =
    cached friendIds u1 map (_ contains u2)

  def friendsOf(userId: ID): Fu[List[User]] =
    cached friendIds userId flatMap UserRepo.byIds map { _ sortBy (_.id) }

  def yes(userId: ID, friendId: ID): Funit =
    Status.fromDb(userId, friendId) flatMap {
      case Status(_, _, Some(friendship), _) ⇒
        fufail("[friend] friendship already exists" format friendship)
      case Status(_, _, _, Some(request)) if request.by(userId) ⇒
        fufail("[friend] request already exists" format request)
      case Status(_, _, _, Some(request)) ⇒
        processRequest(request, true)
      case Status(u1, u2, _, _) ⇒
        $insert(Request.make(u1, u2)) >> refreshNbRequests(u2)
    }

  def no(userId: ID, friendId: ID): Funit =
    Status.fromDb(userId, friendId) flatMap {
      case Status(_, _, Some(friendship), _) ⇒
        $remove(friendship) >> refreshFriendIds(friendship.users: _*)
      case Status(_, _, _, Some(request)) ⇒ processRequest(request, false)
      case _                              ⇒ fufail("[friend] no request nor friendship to revoke")
    }

  def requestsWithUsers(userId: ID): Fu[List[RequestWithUser]] = for {
    requests ← RequestRepo findByFriendId userId
    users ← $find.byOrderedIds[User](requests.map(_.user))
  } yield requests zip users map {
    case (request, user) ⇒ RequestWithUser(request, user)
  }

  private[friend] def makeFriends(u1: ID, u2: ID): Funit =
    FriendRepo.add(u1, u2) >> refreshFriendIds(u1, u2)

  private def processRequest(request: Request, accept: Boolean): Funit =
    $remove(request) >>
      refreshNbRequests(request.friend) >>
      accept ?? makeFriends(request.user, request.friend)

  private def refreshFriendIds(userIds: String*): Funit =
    userIds.toList.map(cached.friendIds.remove).sequence.void

  private def refreshNbRequests(userIds: String*): Funit =
    userIds.toList.map(cached.nbRequests.remove).sequence.void
}
