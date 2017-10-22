package lila.tournament

import scala.concurrent.duration._

import chess.Color
import lila.game.{ Game, Player => GamePlayer, GameRepo, PovRef, Source, PerfPicker }
import lila.user.User

final class AutoPairing(onStart: String => Unit) {

  def apply(tour: Tournament, pairing: Pairing, usersMap: Map[User.ID, User]): Fu[Game] = {
    val user1 = usersMap get pairing.user1 err s"Missing pairing user $pairing"
    val user2 = usersMap get pairing.user2 err s"Missing pairing user $pairing"
    val game1 = Game.make(
      game = chess.Game(
        variantOption = tour.variant.some,
        fen = tour.position.some.filterNot(_.initial).map(_.fen)
      ) |> { g =>
          val turns = g.player.fold(0, 1)
          g.copy(
            clock = tour.clock.toClock.some,
            turns = turns,
            startedAtTurn = turns
          )
        },
      whitePlayer = GamePlayer.white,
      blackPlayer = GamePlayer.black,
      mode = tour.mode,
      variant =
        if (tour.position.initial) tour.variant
        else chess.variant.FromPosition,
      source = Source.Tournament,
      pgnImport = None
    )
    val game2 = game1
      .updatePlayer(Color.White, _.withUser(user1.id, PerfPicker.mainOrDefault(game1)(user1.perfs)))
      .updatePlayer(Color.Black, _.withUser(user2.id, PerfPicker.mainOrDefault(game1)(user2.perfs)))
      .withTournamentId(tour.id)
      .withId(pairing.gameId)
      .start
    (GameRepo insertDenormalized game2) >>- onStart(game2.id) inject game2
  }
}
