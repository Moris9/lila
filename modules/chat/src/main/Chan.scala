package lila.chat

import play.api.libs.json._

sealed trait Chan {
  def typ: String
  def key: String
  def idOption: Option[String]
  def autoActive: Boolean

  override def toString = key
}

sealed abstract class StaticChan(
    val typ: String,
    val name: String) extends Chan {

  val key = typ
  val idOption = none
  val autoActive = false
}

sealed abstract class IdChan(
    val typ: String,
    val autoActive: Boolean) extends Chan {

  val id: String
  def key = s"${typ}_$id"
  def idOption = id.some
}

sealed abstract class AutoActiveChan(typ: String, i: String) extends IdChan(typ, true) {
  val id = i
}

object LichessChan extends StaticChan(Chan.typ.lichess, "Lichess")
object LobbyChan extends StaticChan(Chan.typ.lobby, "Lobby")
object TvChan extends StaticChan(Chan.typ.tv, "TV")
object TournamentLobbyChan extends StaticChan(Chan.typ.tournamentLobby, "Tournament Lobby")

case class GameWatcherChan(i: String) extends AutoActiveChan(Chan.typ.gameWatcher, i)
case class GamePlayerChan(i: String) extends AutoActiveChan(Chan.typ.gamePlayer, i)
case class TournamentChan(i: String) extends AutoActiveChan(Chan.typ.tournament, i)

case class UserChan(u1: String, u2: String) extends IdChan("user", false) {
  val id = List(u1, u2).sorted mkString "-"
  def contains(userId: String) = u1 == userId || u2 == userId
}
object UserChan {
  def apply(id: String): Option[UserChan] = id.split("-") match {
    case Array(u1, u2) ⇒ UserChan(u1, u2).some
    case _             ⇒ none
  }
}

case class NamedChan(chan: Chan, name: String) {

  def toJson = Json.obj(
    "key" -> chan.key,
    "name" -> name)
}

object Chan {

  object typ {
    val lichess = "lichess"
    val lobby = "lobby"
    val tv = "tv"
    val gameWatcher = "gameWatcher"
    val gamePlayer = "gamePlayer"
    val tournamentLobby = "tournamentLobby"
    val tournament = "tournament"
    val user = "user"
  }

  def apply(typ: String, idOption: Option[String]): Option[Chan] = typ match {
    case Chan.typ.lichess         ⇒ LichessChan.some
    case Chan.typ.lobby           ⇒ LobbyChan.some
    case Chan.typ.tv              ⇒ TvChan.some
    case Chan.typ.gameWatcher     ⇒ idOption map GameWatcherChan
    case Chan.typ.gamePlayer      ⇒ idOption map GamePlayerChan
    case Chan.typ.tournamentLobby ⇒ TournamentLobbyChan.some
    case Chan.typ.tournament      ⇒ idOption map TournamentChan
    case Chan.typ.user            ⇒ idOption flatMap UserChan.apply
    case _                        ⇒ none
  }

  def parse(str: String): Option[Chan] = str.split('_').toList match {
    case List(typ) ⇒ apply(typ, none)
    case typ :: id ⇒ apply(typ, id.mkString("_").some)
    case _         ⇒ None
  }
}

