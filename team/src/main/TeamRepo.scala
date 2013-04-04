package lila.team

import lila.db.api._
import tube.teamTube

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._

import org.joda.time.{ DateTime, Period }
import org.scala_tools.time.Imports._

import lila.user.User

object TeamRepo {

  type ID = String

  def owned(id: String, createdBy: String): Fu[Option[Team]] = 
    $find.one($select(id) ++ Json.obj("createdBy" -> createdBy))

  def teamIdsByCreator(userId: String): Fu[List[String]] = 
    $primitive(Json.obj("createdBy" -> userId), "_id")(_.asOpt[String])

  def name(id: String): Fu[Option[String]] = 
    $primitive.one($select(id), "name")(_.asOpt[String])

  def userHasCreatedSince(userId: String, duration: Period): Fu[Boolean] = 
    $count.exists(Json.obj(
      "createdAt" -> $gt(DateTime.now - duration),
      "createdBy" -> userId
    ))

  def incMembers(teamId: String, by: Int): Funit = 
    $update($select(teamId), $inc("nbMembers" -> by))

  def enable(team: Team) = $update.field(team.id, "enabled", true)

  def disable(team: Team) = $update.field(team.id, "enabled", false)

  // TODO
  def addRequest(teamId: String, request: Request): Funit = 
    $update(
      $select(teamId) ++ Json.obj("requests.user" -> $ne(request.user)), 
      $push("requests", request.user))

  val enabledQuery = Json.obj("enabled" -> true)

  val sortPopular = $sort desc "nbMembers"
}
