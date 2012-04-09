package lila

import model._
import memo._
import db.{ GameRepo, RoomRepo }
import chess.{ Color, White, Black }

import scalaz.effects._
import akka.pattern.ask
import akka.dispatch.Future
import akka.util.duration._
import akka.util.Timeout

final class AppApi(
    val gameRepo: GameRepo,
    aliveMemo: AliveMemo,
    gameHubMemo: game.HubMemo,
    messenger: Messenger,
    starter: Starter) extends IOTools {

  implicit val timeout = Timeout(200 millis)

  def show(fullId: String): Future[IO[Map[String, Any]]] =
    (gameHubMemo getFromFullId fullId) ? game.GetVersion map { version ⇒
      for {
        pov ← gameRepo pov fullId
        _ ← aliveMemo.put(pov.game.id, pov.color)
        roomHtml ← messenger render pov.game.id
      } yield Map(
        "version" -> version,
        "roomHtml" -> roomHtml,
        "opponentActivity" -> aliveMemo.activity(pov.game.id, !pov.color),
        "possibleMoves" -> {
          if (pov.game playableBy pov.player)
            pov.game.toChess.situation.destinations map {
              case (from, dests) ⇒ from.key -> (dests.mkString)
            } toMap
          else null
        }
      )
    }

  def join(
    fullId: String,
    url: String,
    messages: String,
    entryData: String): IO[Unit] = for {
    pov ← gameRepo pov fullId
    e1 ← starter.start(pov.game, entryData)
    e2 ← messenger.systemMessages(e1.game, messages) map { evts ⇒
      e1 + RedirectEvent(!pov.color, url) ++ evts
    }
    _ ← save(pov.game, e2)
  } yield ()

  def start(gameId: String, entryData: String): IO[Unit] = for {
    g1 ← gameRepo game gameId
    evented ← starter.start(g1, entryData)
    _ ← save(g1, evented)
  } yield ()

  def rematchAccept(
    gameId: String,
    newGameId: String,
    colorName: String,
    whiteRedirect: String,
    blackRedirect: String,
    entryData: String,
    messageString: String): IO[Unit] = for {
    color ← ioColor(colorName)
    newGame ← gameRepo game newGameId
    g1 ← gameRepo game gameId
    evented = Evented(g1, List(
      RedirectEvent(White, whiteRedirect),
      RedirectEvent(Black, blackRedirect),
      // to tell spectators to reload the table
      ReloadTableEvent(White),
      ReloadTableEvent(Black)))
    _ ← save(g1, evented)
    newEvented ← starter.start(newGame, entryData)
    newEvented2 ← messenger.systemMessages(
      newEvented.game, messageString
    ) map newEvented.++
    _ ← save(newGame, newEvented2)
    _ ← aliveMemo.put(newGameId, !color)
    _ ← aliveMemo.transfer(gameId, !color, newGameId, color)
  } yield ()

  def reloadTable(gameId: String): IO[Unit] = for {
    g1 ← gameRepo game gameId
    evented = Evented(g1, Color.all map ReloadTableEvent)
    _ ← save(g1, evented)
  } yield ()

  def alive(gameId: String, colorName: String): IO[Unit] = for {
    color ← ioColor(colorName)
    _ ← aliveMemo.put(gameId, color)
  } yield ()

  def gameVersion(gameId: String): Future[Int] =
    (gameHubMemo get gameId) ? game.GetVersion map {
      case game.Version(v) ⇒ v
    }

  def activity(gameId: String, colorName: String): Int =
    Color(colorName).fold(aliveMemo.activity(gameId, _), 0)
}
