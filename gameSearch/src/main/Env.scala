package lila.gameSearch

import lila.search.TypeIndexer
import lila.game.{ GameRepo, PgnRepo, Game ⇒ GameModel, Query ⇒ DbQuery }
import lila.db.TubeInColl

import com.typesafe.config.Config
import akka.actor._
import play.api.libs.json.JsObject
import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import org.elasticsearch.action.search.SearchResponse

final class Env(
  config: Config,
  system: ActorSystem,
  esIndexer: EsIndexer) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"

  private implicit val gameTube = lila.game.gameTube

  val indexer: ActorRef = system.actorOf(Props(new Indexer(
    lowLevel = lowLevelIndexer
  )), name = IndexerName)

  lazy val paginatorBuilder = new lila.search.PaginatorBuilder(
    indexer = lowLevelIndexer,
    maxPerPage = PaginatorMaxPerPage,
    converter = responseToGames _)

  lazy val forms = new DataForm

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    import lila.search.actorApi.RebuildAll
    private implicit def timeout = makeTimeout minutes 20
    def process = {
      case "game" :: "search" :: "reset" :: Nil ⇒
        (lowLevelIndexer ? RebuildAll) inject "Game search index rebuilt"
    }
  }

  private val lowLevelIndexer: ActorRef = system.actorOf(Props(new TypeIndexer(
    es = esIndexer,
    indexName = IndexName,
    typeName = TypeName,
    mapping = Game.jsonMapping,
    indexQuery = indexQuery _
  )), name = IndexerName + "-low-level")

  private def responseToGames(response: SearchResponse): Fu[List[GameModel]] = 
    lila.db.api.$find.byOrderedIds[String, GameModel] {
      response.hits.hits.toList map (_.id)
    }

  private def indexQuery(sel: JsObject): Funit = {
    import play.api.libs.json._
    import play.api.libs.concurrent.Execution.Implicits._
    import play.api.libs.iteratee._
    import lila.db.api._
    import lila.db.Implicits.LilaPimpedQueryBuilder
    val selector = DbQuery.frozen ++ sel
    val cursor = $query(selector).sort(DbQuery.sortCreated).cursor //limit 3000
    val size = $count(selector).await
    var nb = 0
    cursor.enumerateBulks(5000) run {
      Iteratee foreach { (gameOptions: Iterator[Option[GameModel]]) ⇒
        val games = gameOptions.flatten
        nb = nb + games.size
        if (size > 1000) println(s"Index $nb of $size games")
        // #TODO fetch all pgns in one request
        val pgns = games.map(g ⇒ (PgnRepo get g.id).await)
        esIndexer bulk {
          games zip pgns map {
            case (game, pgn) ⇒ esIndexer.index_prepare(
              IndexName,
              TypeName,
              game.id,
              Json stringify Game.from(game, pgn)
            ).request
          } toList
        }
      }
    }
  } 
}

object Env {

  lazy val current = "[gameSearch] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "gameSearch",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current),
    esIndexer = lila.search.Env.current.esIndexer)
}
