package lila
package wiki

import core.Settings

import com.mongodb.casbah.MongoCollection

final class WikiEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val pageRepo = new PageRepo(mongodb(WikiCollectionPage))

  lazy val api = new Api(pageRepo = pageRepo)
  
  lazy val fetch = new Fetch(
    gitUrl = WikiGitUrl,
    pageRepo = pageRepo)
}
