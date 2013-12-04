package lila.game

import chess.{ Clock, Pos }
import Game.BSONFields._
import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.ByteArray

private[game] object GameDiff {

  type Set = (String, BSONValue)
  type Unset = (String, BSONBoolean)

  def apply(a: Game, b: Game): (List[Set], List[Unset]) = {

    val setBuilder = scala.collection.mutable.ListBuffer[Set]()
    val unsetBuilder = scala.collection.mutable.ListBuffer[Unset]()

    def d[A, B <: BSONValue](name: String, getter: Game ⇒ A, toBson: A ⇒ B) {
      val (va, vb) = (getter(a), getter(b))
      if (va != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> BSONBoolean(true))
        else setBuilder += name -> toBson(vb)
      }
    }

    def dOpt[A, B <: BSONValue](name: String, getter: Game ⇒ A, toBson: A ⇒ Option[B]) {
      val (va, vb) = (getter(a), getter(b))
      if (va != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> BSONBoolean(true))
        else toBson(vb) match {
          case None    ⇒ unsetBuilder += (name -> BSONBoolean(true))
          case Some(x) ⇒ setBuilder += name -> x
        }
      }
    }

    val w = lila.db.BSON.writer

    d(binaryPieces, _.binaryPieces, ByteArray.ByteArrayBSONHandler.write)
    d(status, _.status.id, w.int)
    d(turns, _.turns, w.int)
    d(castleLastMoveTime, _.castleLastMoveTime, CastleLastMoveTime.castleLastMoveTimeBSONHandler.write)
    dOpt(check, _.check, (x: Option[chess.Pos]) ⇒ w.map(x map (_.toString)))
    dOpt(positionHashes, _.positionHashes, w.strO)
    dOpt(clock, _.clock, (o: Option[Clock]) ⇒ o map { c ⇒ Game.clockBSONHandler.write(_ ⇒ c) })
    for (i ← 0 to 1) {
      import Player.BSONFields._
      val name = s"p.$i."
      val player: Game ⇒ Player = if (i == 0) (_.whitePlayer) else (_.blackPlayer)
      dOpt(name + isWinner, player(_).isWinner, w.map[Option, Boolean, BSONBoolean])
      dOpt(name + lastDrawOffer, player(_).lastDrawOffer, w.map[Option, Int, BSONInteger])
      dOpt(name + isOfferingDraw, player(_).isOfferingDraw, w.boolO)
      dOpt(name + isOfferingRematch, player(_).isOfferingRematch, w.boolO)
      dOpt(name + isProposingTakeback, player(_).isProposingTakeback, w.boolO)
      dOpt(name + blurs, player(_).blurs, w.intO)
      dOpt(name + moveTimes, player(_).moveTimes, w.strO)
    }

    (setBuilder.toList, unsetBuilder.toList)
  }
}
