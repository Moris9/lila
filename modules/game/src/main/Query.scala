package lila.game

import chess.{ Color, Status }
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json._

import lila.db.api._
import lila.user.User

object Query {

  import Game.ShortFields._

  val all: JsObject = $select.all

  val rated: JsObject = Json.obj("ra" -> true)

  def rated(u: String): JsObject = user(u) ++ rated

  val started: JsObject = Json.obj("s" -> $gte(Status.Started.id))

  def started(u: String): JsObject = user(u) ++ started

  val playable = Json.obj("s" -> $lt(Status.Aborted.id))

  val mate = Json.obj("s" -> Status.Mate.id)

  val draw: JsObject = Json.obj("s" -> $in(Seq(Status.Draw.id, Status.Stalemate.id)))

  def draw(u: String): JsObject = user(u) ++ draw

  val finished = Json.obj("s" -> $gte(Status.Mate.id))

  val notFinished: JsObject = Json.obj("s" -> $lte(Status.Started.id))

  def notFinished(u: String): JsObject = user(u) ++ notFinished

  val frozen = Json.obj("s" -> $gte(Status.Mate.id))

  val popular = Json.obj("bm" -> $gt(0))

  val imported = Json.obj("me.so" -> Source.Import.id)

  def pgnImport(pgn: String) = imported ++ Json.obj("me.pgni.pgn" -> pgn)

  def clock(c: Boolean) = Json.obj("c" -> $exists(c))

  def user(u: String) = Json.obj("uids" -> u)

  // use the uids index
  def win(u: String) = user(u) ++ Json.obj("wid" -> u)

  def loss(u: String) = user(u) ++
    Json.obj("s" -> $in(Status.finishedWithWinner map (_.id))) ++
    Json.obj("wid" -> $ne(u))

  def opponents(u1: User, u2: User) =
    Json.obj("uids" -> $all(List(u1, u2).sortBy(_.count.game).map(_.id)))

  def turnsGt(nb: Int) = Json.obj("t" -> $gt(nb))

  def candidatesToAutofinish = playable ++ clock(true) ++ Json.obj(
    createdAt -> $gt(DateTime.now - 1.day),
    updatedAt -> $lt(DateTime.now - 2.hour))

  def abandoned = notFinished ++ Json.obj(updatedAt -> $lt(Game.abandonedDate))

  val sortCreated = $sort desc createdAt

  val sortPopular = $sort desc "bm"

}
