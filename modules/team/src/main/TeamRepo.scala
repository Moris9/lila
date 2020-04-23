package lila.team

import org.joda.time.{ DateTime, Period }
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api._
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User

final class TeamRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def byOrderedIds(ids: Seq[Team.ID]) = coll.byOrderedIds[Team, Team.ID](ids)(_.id)

  def byLeader(id: Team.ID, leaderId: User.ID): Fu[Option[Team]] =
    coll.one[Team]($id(id) ++ $doc("leaders" -> leaderId))

  def teamIdsByLeader(userId: User.ID): Fu[List[String]] =
    coll.distinctEasy[String, List]("_id", $doc("leader" -> userId))

  def leadersOf(teamId: Team.ID): Fu[Set[User.ID]] =
    coll.primitiveOne[Set[User.ID]]($id(teamId), "leaders").dmap(~_)

  def setLeaders(teamId: String, leaders: Set[User.ID]) =
    coll.updateField($id(teamId), "leaders", leaders)

  def leads(teamId: String, userId: User.ID) =
    coll.exists($id(teamId) ++ $doc("leaders" -> userId))

  def name(id: String): Fu[Option[String]] =
    coll.primitiveOne[String]($id(id), "name")

  def userHasCreatedSince(userId: String, duration: Period): Fu[Boolean] =
    coll.exists(
      $doc(
        "createdAt" $gt DateTime.now.minus(duration),
        "createdBy" -> userId
      )
    )

  def incMembers(teamId: String, by: Int): Funit =
    coll.update.one($id(teamId), $inc("nbMembers" -> by)).void

  def enable(team: Team) = coll.updateField($id(team.id), "enabled", true)

  def disable(team: Team) = coll.updateField($id(team.id), "enabled", false)

  def addRequest(teamId: String, request: Request): Funit =
    coll.update
      .one(
        $id(teamId) ++ $doc("requests.user" $ne request.user),
        $push("requests" -> request.user)
      )
      .void

  def cursor =
    coll.ext
      .find($doc("enabled" -> true))
      .cursor[Team](ReadPreference.secondaryPreferred)

  val enabledQuery = $doc("enabled" -> true)

  val sortPopular = $sort desc "nbMembers"
}
