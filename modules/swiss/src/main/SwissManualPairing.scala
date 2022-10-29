package lila.swiss

import lila.db.dsl._
import lila.swiss.BsonHandlers._
import lila.user.User

final private class SwissManualPairing(colls: SwissColls)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(swiss: Swiss): Option[Fu[List[SwissPairing.ByeOrPending]]] =
    swiss.settings.manualPairings.some.filter(_.nonEmpty) map { str =>
      SwissPlayer.fields { p =>
        val selectPresentPlayers = $doc(p.swissId -> swiss.id, p.absent $ne true)
        colls.player.distinctEasy[User.ID, Set](p.userId, selectPresentPlayers) map { presentUserIds =>
          val pairings = str.linesIterator.flatMap {
            _.trim.toLowerCase.split(' ').map(_.trim) match {
              case Array(u1, u2) if presentUserIds(u1) && presentUserIds(u2) && u1 != u2 =>
                SwissPairing.Pending(u1, u2).some
              case _ => none
            }
          }.toList
          val paired   = pairings.flatMap { p => List(p.white, p.black) }.toSet
          val unpaired = presentUserIds diff paired
          val byes     = unpaired map { u => SwissPairing.Bye(u) }
          pairings.map(Right.apply) ::: byes.toList.map(Left.apply)
        }
      }
    }
}
