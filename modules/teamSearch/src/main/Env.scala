package lila.teamSearch

import akka.actor._
import com.typesafe.config.Config

import lila.db.api.{ $find, $cursor }
import lila.team.tube.teamTube

final class Env(
    config: Config,
    client: lila.search.ESClient,
    system: ActorSystem) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"

  private val indexer: ActorRef = system.actorOf(Props(new Indexer(
    client = client,
    indexName = IndexName,
    typeName = TypeName
  )), name = IndexerName)

  def apply(text: String, page: Int) = {
    val query = Query(s"$IndexName/$TypeName", text)
    paginatorBuilder(query, page)
  }

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    private implicit def timeout = makeTimeout minutes 20
    def process = {
      case "team" :: "search" :: "reset" :: Nil =>
        (indexer ? lila.search.actorApi.Reset) inject "Team search index rebuilt"
    }
  }

  private lazy val paginatorBuilder = new lila.search.PaginatorBuilder(
    indexer = indexer,
    maxPerPage = PaginatorMaxPerPage,
    converter = $find.byOrderedIds[lila.team.Team] _
  )
}

object Env {

  lazy val current = "teamSearch" boot new Env(
    config = lila.common.PlayApp loadConfig "teamSearch",
    client = lila.search.Env.current.client,
    system = lila.common.PlayApp.system)
}
