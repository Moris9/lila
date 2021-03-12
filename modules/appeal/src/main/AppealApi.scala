package lila.appeal

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime
import lila.user.Holder

final class AppealApi(
    coll: Coll
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def mine(me: User): Fu[Option[Appeal]] = coll.byId[Appeal](me.id)

  def get(user: User) = coll.byId[Appeal](user.id)

  def exists(user: User) = coll.exists($id(user.id))

  def post(text: String, me: User) =
    mine(me) flatMap {
      case None =>
        val appeal =
          Appeal(
            _id = me.id,
            msgs = Vector(
              AppealMsg(
                by = me.id,
                text = text,
                at = DateTime.now
              )
            ),
            status = Appeal.Status.Unread,
            createdAt = DateTime.now,
            updatedAt = DateTime.now,
            firstUnrepliedAt = DateTime.now
          )
        coll.insert.one(appeal) inject appeal
      case Some(prev) =>
        val appeal = prev.post(text, me)
        coll.update.one($id(appeal.id), appeal) inject appeal
    }

  def reply(text: String, prev: Appeal, mod: Holder) = {
    val appeal = prev.post(text, mod.user)
    coll.update.one($id(appeal.id), appeal) inject appeal
  }

  def countUnread = coll.countSel($doc("status" -> Appeal.Status.Unread.key))

  def queue: Fu[List[Appeal]] =
    coll
      .find($doc("status" -> Appeal.Status.Unread.key))
      .sort($doc("firstUnrepliedAt" -> 1))
      .cursor[Appeal]()
      .list(12) flatMap { unreads =>
      coll
        .find($doc("status" $ne Appeal.Status.Unread.key))
        .sort($doc("firstUnrepliedAt" -> -1))
        .cursor[Appeal]()
        .list(20 - unreads.size) map {
        unreads ::: _
      }
    }

  def read(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.read).void

  def unread(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.unread).void

  def toggleMute(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.toggleMute).void

  def readById(userId: User.ID) =
    coll.byId[Appeal](userId) flatMap { _ ?? read }

  def unreadById(userId: User.ID) =
    coll.byId[Appeal](userId) flatMap { _ ?? unread }
}
