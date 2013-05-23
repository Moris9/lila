package lila.relation

import lila.db.Implicits._
import lila.db.api._
import tube.relationTube
import lila.common.PimpedJson._

import play.api.libs.json._

private[relation] object RelationRepo {

  def relation(id: ID): Fu[Option[Relation]] =
    $primitive.one($select byId id, "r")(_.asOpt[Boolean])

  def relation(u1: ID, u2: ID): Fu[Option[Relation]] = relation(makeId(u1, u2))

  def followers(userId: ID) = relaters(userId, Follow)
  def following(userId: ID) = relating(userId, Follow)

  def blockers(userId: ID) = relaters(userId, Block)
  def blocking(userId: ID) = relating(userId, Block)

  private def relaters(userId: ID, relation: Relation): Fu[Set[ID]] =
    $projection(Json.obj("u2" -> userId), Seq("u1", "r")) { obj ⇒
      obj str "u1" map { _ -> ~(obj boolean "r") }
    } map (_.filter(_._2 == relation).map(_._1).toSet)

  private def relating(userId: ID, relation: Relation): Fu[Set[ID]] =
    $projection(Json.obj("u1" -> userId), Seq("u2", "r")) { obj ⇒
      obj str "u2" map { _ -> ~(obj boolean "r") }
    } map (_.filter(_._2 == relation).map(_._1).toSet)

  def follow(u1: ID, u2: ID): Funit = save(u1, u2, Follow)
  def unfollow(u1: ID, u2: ID): Funit = remove(u1, u2)
  def block(u1: ID, u2: ID): Funit = save(u1, u2, Block)
  def unblock(u1: ID, u2: ID): Funit = remove(u1, u2)

  private def save(u1: ID, u2: ID, relation: Relation): Funit = $save(
    makeId(u1, u2),
    Json.obj("u1" -> u1, "u2" -> u2, "r" -> relation)
  )

  def remove(u1: ID, u2: ID): Funit = $remove byId makeId(u1, u2)

  private def makeId(u1: String, u2: String) = u1 + "/" + u2
}
