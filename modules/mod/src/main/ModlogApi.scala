package lila.mod

import lila.db.api._
import lila.db.Implicits._
import tube.modlogTube

final class ModlogApi {

  def engine(mod: String, user: String, v: Boolean) = add {
    Modlog(mod, user.some, v.fold(Modlog.engine, Modlog.unengine))
  }

  def mute(mod: String, user: String, v: Boolean) = add {
    Modlog(mod, user.some, v.fold(Modlog.mute, Modlog.unmute))
  }

  def ban(mod: String, user: String) = add {
    Modlog(mod, user.some, Modlog.ipban)
  }

  def ipban(mod: String, ip: String) = add {
    Modlog(mod, none, Modlog.ipban, ip.some)
  }

  def deletePost(mod: String, user: Option[String], author: Option[String], ip: Option[String], text: String) = add {
    Modlog(mod, user, Modlog.deletePost, details = Some(
      author.zmap(_ + " ") + ip.zmap(_ + " ") + text.take(140)
    ))
  }

  def toggleCloseTopic(mod: String, categ: String, topic: String, closed: Boolean) = add {
    Modlog(mod, none, closed ? Modlog.closeTopic | Modlog.openTopic, details = Some(
      categ + " / " + topic
    ))
  }

  def recent = $find($query($select.all) sort $sort.naturalDesc, 100)

  private def add(m: Modlog): Funit = $insert(m)
}
