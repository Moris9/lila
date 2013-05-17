package lila.forum

import lila.db.Implicits._
import lila.db.api._
import tube.postTube

import play.api.libs.json.Json

object PostRepo extends PostRepo(false) {

  def apply(troll: Boolean): PostRepo = troll.fold(PostRepoTroll, PostRepo)
}

object PostRepoTroll extends PostRepo(true)

sealed abstract class PostRepo(troll: Boolean) {

  private lazy val trollFilter = troll.fold(
    Json.obj(), 
    Json.obj("troll" -> false)
  )

  def isFirstPost(topicId: String, postId: String): Fu[Boolean] =
    $primitive.one(
      selectTopic(topicId),
      "_id",
      _ sort $sort.createdAsc
    )(_.asOpt[String]) map { _.zmap(postId ==) }

  def countByTopics(topics: List[String]): Fu[Int] =
    $count(selectTopics(topics)) 

  def lastByTopics(topics: List[String]): Fu[Option[Post]] =
    $find.one($query(selectTopics(topics)) sort $sort.createdDesc)

  def recentInCategs(nb: Int)(categIds: List[String]): Fu[List[Post]] =
    $find($query(selectCategs(categIds)) sort $sort.createdDesc, nb)

  def removeByTopic(topicId: String): Fu[Unit] =
    $remove(selectTopic(topicId))

  def selectTopic(topicId: String) = Json.obj("topicId" -> topicId) ++ trollFilter
  def selectTopics(topicIds: List[String]) = Json.obj("topicId" -> $in(topicIds)) ++ trollFilter

  def selectCategs(categIds: List[String]) = Json.obj("categId" -> $in(categIds)) ++ trollFilter

  def sortQuery = $sort.createdAsc
}
