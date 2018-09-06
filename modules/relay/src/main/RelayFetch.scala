package lila.relay

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import io.lemonlabs.uri.Url
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import scala.concurrent.duration._

import chess.format.pgn.Tags
import lila.base.LilaException
import lila.study.MultiPgn
import lila.tree.Node.Comments
import Relay.Sync.Upstream

private final class RelayFetch(
    sync: RelaySync,
    api: RelayApi,
    formatApi: RelayFormatApi,
    chapterRepo: lila.study.ChapterRepo
) extends Actor {

  val frequency = 1.seconds

  override def preStart: Unit = {
    logger.info("Start RelaySync")
    context setReceiveTimeout 20.seconds
    context.system.scheduler.scheduleOnce(10.seconds)(scheduleNext)
  }

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(frequency, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "RelaySync timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Tick => api.toSync.flatMap { relays =>
      lila.mon.relay.ongoing(relays.size)
      relays.map { relay =>
        if (relay.sync.ongoing) processRelay(relay) flatMap { newRelay =>
          api.update(relay)(_ => newRelay)
        }
        else if (relay.hasStarted) {
          logger.info(s"Finish by lack of activity $relay")
          api.update(relay)(_.finish)
        } else if (relay.shouldGiveUp) {
          logger.info(s"Finish for lack of start $relay")
          api.update(relay)(_.finish)
        } else fuccess(relay)
      }.sequenceFu addEffectAnyway scheduleNext
    }
  }

  // no writing the relay; only reading!
  def processRelay(relay: Relay): Fu[Relay] =
    if (!relay.sync.playing) fuccess(relay.withSync(_.play))
    else doProcess(relay) flatMap { games =>
      sync(relay, games)
        .chronometer.mon(_.relay.sync.duration.each).result
        .withTimeout(1 second, SyncResult.Timeout)(context.system) map { res =>
          res -> relay.withSync(_ addLog SyncLog.event(res.moves, none))
        }
    } recover {
      case e: Exception => (e match {
        case res @ SyncResult.Timeout =>
          logger.info(s"Sync timeout $relay")
          res
        case _ =>
          // logger.info(s"Sync error $relay ${e.getMessage take 80}")
          logger.error(s"Sync error $relay ${e.getMessage take 80}", e)
          SyncResult.Error(e.getMessage)
      }) -> relay.withSync(_ addLog SyncLog.event(0, e.some))
    } flatMap {
      case (result, newRelay) => afterSync(result, newRelay)
    }

  def afterSync(result: SyncResult, relay: Relay): Fu[Relay] = {
    lila.mon.relay.sync.result(result.reportKey)()
    result match {
      case SyncResult.Ok(0, games) =>
        if (games.size > 1 && games.forall(_.finished)) {
          logger.info(s"Finish because all games are over $relay")
          fuccess(relay.finish)
        } else continueRelay(relay)
      case SyncResult.Ok(nbMoves, games) =>
        lila.mon.relay.moves(nbMoves)
        continueRelay(relay.ensureStarted.resume)
      case _ => continueRelay(relay)
    }
  }

  def continueRelay(r: Relay): Fu[Relay] =
    (if (r.sync.log.alwaysFails) fuccess(60) else (r.sync.delay match {
      case Some(delay) => fuccess(delay)
      case None => api.getNbViewers(r) map { nb =>
        (18 - nb) atLeast 8
      }
    })) map { seconds =>
      r.withSync(_.copy(nextAt = DateTime.now plusSeconds {
        seconds atLeast { if (r.sync.log.isOk) 5 else 15 }
      } some))
    }

  import RelayFetch.GamesSeenBy

  private def doProcess(relay: Relay): Fu[RelayGames] =
    cache getIfPresent relay.sync.upstream match {
      case Some(GamesSeenBy(games, seenBy)) if !seenBy(relay.id) =>
        cache.put(relay.sync.upstream, GamesSeenBy(games, seenBy + relay.id))
        games
      case x =>
        val games = doFetch(relay.sync.upstream, RelayFetch.maxChapters(relay))
        cache.put(relay.sync.upstream, GamesSeenBy(games, Set(relay.id)))
        games
    }

  // private def dgtManyFiles(dir: String, max: Int, format: DgtMany): Fu[MultiPgn] = {
  //   val indexFile = s"$dir/${format.indexFile}"
  //   httpGet(indexFile) flatMap {
  //     case res if res.status == 200 => roundReads reads res.json match {
  //       case JsError(err) => fufail(err.toString)
  //       case JsSuccess(round, _) => round.pairings.zipWithIndex.map {
  //         case (pairing, i) =>
  //           val number = i + 1
  //           val gameUrl = s"$dir/${format.gameFile(number)}"
  //           httpGet(gameUrl).flatMap {
  //             case res if res.status == 200 => fuccess(number -> format.toPgn(res, pairing))
  //             case res => fufail(s"[${res.status}] $gameUrl")
  //           }
  //       }.sequenceFu map { results =>
  //         MultiPgn(results.sortBy(_._1).map(_._2).toList)
  //       }
  //     }
  //     case res => fufail(s"[${res.status}] $indexFile")
  //   }
  // }

  private val cache: Cache[Upstream, GamesSeenBy] = Scaffeine()
    .expireAfterWrite(30.seconds)
    .build[Upstream, GamesSeenBy]

  private def doFetch(upstream: Upstream, max: Int): Fu[RelayGames] =
    formatApi.get(upstream.url) flatMap {
      _.fold[Fu[MultiPgn]](fufail("Cannot find any DGT compatible files")) {
        case RelayFormat.SingleFile(doc) => httpGet(doc.url) map { body =>
          doc.format match {
            // all games in a single PGN file
            case RelayFormat.DocFormat.Pgn => MultiPgn.split(body, max)
            // maybe a single JSON game? Why not
            case RelayFormat.DocFormat.Json => MultiPgn(List(RelayFetch.jsonToPgn(body)))
          }
        }
        case f: RelayFormat.ManyFiles => ???
      }
    } flatMap RelayFetch.multiPgnToGames.apply

  private def httpGet(url: Url): Fu[String] =
    WS.url(url.toString).withRequestTimeout(4.seconds.toMillis).get().flatMap {
      case res if res.status == 200 => fuccess(res.body)
      case res => fufail(s"[${res.status}]")
    }
}

private object RelayFetch {

  case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[Relay.Id])

  def maxChapters(relay: Relay) =
    lila.study.Study.maxChapters * (if (relay.official) 2 else 1)

  private object DgtJson {
    case class PairingPlayer(fname: Option[String], mname: Option[String], lname: Option[String], title: Option[String]) {
      def fullName = some {
        List(fname, mname, lname).flatten mkString " "
      }.filter(_.nonEmpty)
    }
    case class RoundJsonPairing(white: PairingPlayer, black: PairingPlayer, result: String) {
      import chess.format.pgn._
      def tags = Tags(List(
        white.fullName map { v => Tag(_.White, v) },
        white.title map { v => Tag(_.WhiteTitle, v) },
        black.fullName map { v => Tag(_.Black, v) },
        black.title map { v => Tag(_.BlackTitle, v) },
        Tag(_.Result, result).some
      ).flatten)
    }
    case class RoundJson(pairings: List[RoundJsonPairing])
    implicit val pairingPlayerReads = Json.reads[PairingPlayer]
    implicit val roundPairingReads = Json.reads[RoundJsonPairing]
    implicit val roundReads = Json.reads[RoundJson]

    case class GameJson(moves: List[String], result: Option[String])
    implicit val gameReads = Json.reads[GameJson]
  }
  import DgtJson._

  private sealed abstract class DgtMany(val indexFile: String, val gameFile: Int => String, val toPgn: (WSResponse, RoundJsonPairing) => String)
  private object DgtMany {
    case object RoundPgn extends DgtMany("round.json", n => s"game-$n.pgn", (r, _) => r.body)
    case object Indexjson extends DgtMany("index.json", n => s"game-$n.pgn", {
      case (res, pairing) => res.json.validate[GameJson] match {
        case JsSuccess(game, _) =>
          val moves = game.moves.map(_ split ' ') map { move =>
            chess.format.pgn.Move(
              san = ~move.headOption,
              secondsLeft = move.lift(1).map(_.takeWhile(_.isDigit)) flatMap parseIntOption
            )
          } mkString " "
          s"${pairing.tags}\n\n$moves"
        case JsError(err) => ""
      }
    })
  }

  private def jsonToPgn(str: String, extraTags: Tags = Tags.empty) = Json.parse(str).validate[GameJson] match {
    case JsSuccess(game, _) =>
      val moves = game.moves.map(_ split ' ') map { move =>
        chess.format.pgn.Move(
          san = ~move.headOption,
          secondsLeft = move.lift(1).map(_.takeWhile(_.isDigit)) flatMap parseIntOption
        )
      } mkString " "
      s"${extraTags}\n\n$moves"
    case JsError(err) => ""
  }

  private object multiPgnToGames {

    import scala.util.{ Try, Success, Failure }
    import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }

    def apply(multiPgn: MultiPgn): Fu[List[RelayGame]] =
      multiPgn.value.foldLeft[Try[(List[RelayGame], Int)]](Success(List.empty -> 0)) {
        case (Success((acc, index)), pgn) => pgnCache.get(pgn) map { f =>
          val game = f(index)
          if (game.isEmpty) acc -> index
          else (game :: acc, index + 1)
        }
        case (acc, _) => acc
      }.future.map(_._1.reverse)

    private val pgnCache: LoadingCache[String, Try[Int => RelayGame]] = Scaffeine()
      .expireAfterAccess(2 minutes)
      .build(compute)

    private def compute(pgn: String): Try[Int => RelayGame] =
      lila.study.PgnImport(pgn, Nil).fold(
        err => Failure(LilaException(err)),
        res => Success(index => RelayGame(
          index = index,
          tags = res.tags,
          variant = res.variant,
          root = res.root.copy(
            comments = Comments.empty,
            children = res.root.children.updateMainline(_.copy(comments = Comments.empty))
          ),
          end = res.end
        ))
      )
  }
}
