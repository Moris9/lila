package lila
package forum

import user.{ User, UserRepo }
import core.Settings

import com.mongodb.casbah.MongoCollection
import com.mongodb.DBRef

final class ForumEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    userRepo: UserRepo,
    val userDbRef: User ⇒ DBRef) {

  import settings._

  lazy val categRepo = new CategRepo(mongodb(MongoCollectionForumCateg))

  lazy val topicRepo = new TopicRepo(mongodb(MongoCollectionForumTopic))

  lazy val postRepo = new PostRepo(mongodb(MongoCollectionForumPost))

  lazy val categApi = new CategApi(this)

  lazy val topicApi = new TopicApi(this, ForumTopicMaxPerPage)

  lazy val postApi = new PostApi(this, ForumPostMaxPerPage)

  lazy val denormalize = topicApi.denormalize flatMap { _ ⇒ categApi.denormalize }
}
