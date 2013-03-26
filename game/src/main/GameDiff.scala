package lila.game

import play.api.libs.json._
import Json.JsValueWrapper
import org.joda.time.DateTime

// TODO it works, but it could be more functional
private[game] object GameDiff extends lila.db.api.Free {

  type Set = (String, JsValueWrapper)
  type Unset = String

  def apply(a: RawGame, b: RawGame): (List[Set], List[Unset]) = {

    val setBuilder = scala.collection.mutable.ListBuffer[Set]()
    val unsetBuilder = scala.collection.mutable.ListBuffer[Unset]()

    def d[A: Writes](name: String, f: RawGame ⇒ A) {
      val (va, vb) = (f(a), f(b))
      if (va != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += name
        else setBuilder += name -> vb
      }
    }

    d("s", _.s)
    d("t", _.t)
    d("lm", _.lm) // lastMove
    d("ck", _.ck) // check
    d("ph", _.ph) // positionHashes
    d("cs", _.cs) // castles
    d("lmt", _.lmt)
    for (i ← 0 to 1) {
      val name = "p." + i + "."
      d(name + "ps", _.p(i).ps) // pieces
      d(name + "w", _.p(i).w) // winner
      d(name + "lastDrawOffer", _.p(i).lastDrawOffer)
      d(name + "isOfferingDraw", _.p(i).isOfferingDraw)
      d(name + "isOfferingRematch", _.p(i).isOfferingRematch)
      d(name + "isProposingTakeback", _.p(i).isProposingTakeback)
      d(name + "bs", _.p(i).bs) // blurs
      d(name + "mts", _.p(i).mts) // movetimes
    }
    a.c foreach { c ⇒
      d("c.c", _.c.get.c)
      d("c.w", _.c.get.w)
      d("c.b", _.c.get.b)
      d("c.t", _.c.get.t) // timer
    }

    (addUa(setBuilder.toList), unsetBuilder.toList)
  }

  private def addUa(sets: List[Set]): List[Set] = sets match {
    case Nil  ⇒ Nil
    case sets ⇒ ("ua" -> Json.toJsFieldJsValueWrapper($date(DateTime.now))) :: sets
  }
}
