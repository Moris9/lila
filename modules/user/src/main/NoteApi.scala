package lila.user

import lila.db.dsl._
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference

case class Note(
  _id: String,
  from: String,
  to: String,
  text: String,
  troll: Boolean,
  mod: Boolean,
  date: DateTime)

case class UserNotes(user: User, notes: List[Note])

final class NoteApi(
    coll: Coll,
    timeline: akka.actor.ActorSelection,
    bus: lila.common.Bus) {

  import reactivemongo.bson._
  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val noteBSONHandler = Macros.handler[Note]

  def get(user: User, me: User, myFriendIds: Set[String], isMod: Boolean): Fu[List[Note]] =
    coll.find(
      $doc("to" -> user.id) ++
        me.troll.fold($empty, $doc("troll" -> false)) ++
        isMod.fold(
          $or(
            "from" $in (myFriendIds + me.id),
            "mod" $eq true
          ),
          $doc(
            "from" $in (myFriendIds + me.id),
            "mod" -> false
          )
        )
    ).sort($doc("date" -> -1)).cursor[Note]().gather[List](20)

  def byUserIdsForMod(ids: List[User.ID]): Fu[List[Note]] =
    coll.find($doc(
      "to" $in ids,
      "mod" -> true
    )).sort($doc("date" -> -1))
      .cursor[Note](readPreference = ReadPreference.secondaryPreferred)
      .gather[List](ids.size * 5)

  def write(to: User, text: String, from: User, modOnly: Boolean) = {

    val note = Note(
      _id = ornicar.scalalib.Random nextString 8,
      from = from.id,
      to = to.id,
      text = text,
      troll = from.troll,
      mod = modOnly,
      date = DateTime.now)

    coll.insert(note) >>- {
      import lila.hub.actorApi.timeline.{ Propagate, NoteCreate }
      timeline ! {
        Propagate(NoteCreate(note.from, note.to)) toFriendsOf from.id exceptUser note.to modsOnly note.mod
      }
      bus.publish(lila.hub.actorApi.user.Note(
        from = from.username,
        to = to.username,
        text = note.text,
        mod = modOnly
      ), 'userNote)
    }
  }
}
