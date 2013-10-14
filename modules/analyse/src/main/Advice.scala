package lila.analyse

import chess.format.Nag

private[analyse] sealed trait Advice {
  def nag: Nag
  def info: Info
  def prev: Info

  def ply = info.ply
  def turn = info.turn
  def color = info.color
  def score = info.score

  def makeComment(withScore: Boolean): String = {
    score.ifTrue(withScore) ?? { score ⇒ s"(${score.showPawns}) " }
  } + (this match {
    case MateAdvice(seq, _, _, _) ⇒ seq.desc
    case CpAdvice(nag, _, _)      ⇒ nag.toString
  }) + "." + {
    info.variation.headOption ?? { move ⇒ s" Best was $move." }
  }
}

private[analyse] object Advice {

  def apply(prev: Info, info: Info): Option[Advice] =
    if (info.hasVariation) CpAdvice(prev, info) orElse MateAdvice(prev, info)
    else None
}

private[analyse] case class CpAdvice(
  nag: Nag,
  info: Info,
  prev: Info) extends Advice

private[analyse] object CpAdvice {

  private val cpNags = List(
    300 -> Nag.Blunder,
    100 -> Nag.Mistake,
    50 -> Nag.Inaccuracy)

  def apply(prev: Info, info: Info): Option[CpAdvice] = for {
    cp ← prev.score map (_.ceiled.centipawns)
    infoCp ← info.score map (_.ceiled.centipawns)
    delta = (infoCp - cp) |> { d ⇒ info.color.fold(-d, d) }
    nag ← cpNags find { case (d, n) ⇒ d <= delta } map (_._2)
  } yield CpAdvice(nag, info, prev)
}

private[analyse] sealed abstract class MateSequence(val desc: String)
private[analyse] case class MateDelayed(before: Int, after: Int) extends MateSequence(
  desc = "Detected checkmate in %s moves, but player moved for mate in %s".format(before, after + 1))
private[analyse] case object MateLost extends MateSequence(
  desc = "Lost forced checkmate sequence")
private[analyse] case object MateCreated extends MateSequence(
  desc = "Checkmate is now unavoidable")

private[analyse] object MateSequence {
  def apply(prev: Option[Int], next: Option[Int]): Option[MateSequence] =
    (prev, next).some collect {
      case (None, Some(n)) if n < 0                        ⇒ MateCreated
      case (Some(p), None) if p > 0                        ⇒ MateLost
      case (Some(p), Some(n)) if (p > 0) && (n < 0)        ⇒ MateLost
      case (Some(p), Some(n)) if p > 0 && n >= p && p <= 5 ⇒ MateDelayed(p, n)
    }
}
private[analyse] case class MateAdvice(
  sequence: MateSequence,
  nag: Nag,
  info: Info,
  prev: Info) extends Advice
private[analyse] object MateAdvice {

  def apply(prev: Info, info: Info): Option[MateAdvice] = {
    def reverse(m: Int) = info.color.fold(m, -m)
    def prevScore = reverse(prev.score ?? (_.centipawns))
    def nextScore = reverse(info.score ?? (_.centipawns))
    MateSequence(prev.mate map reverse, info.mate map reverse) map { sequence ⇒
      val nag = sequence match {
        case MateCreated if prevScore > 0    ⇒ Nag.Blunder
        case MateCreated if prevScore > -500 ⇒ Nag.Mistake
        case MateCreated                     ⇒ Nag.Inaccuracy
        case MateLost if nextScore < 500     ⇒ Nag.Blunder
        case MateLost if nextScore < 1000    ⇒ Nag.Mistake
        case MateLost                        ⇒ Nag.Inaccuracy
        case _: MateDelayed                  ⇒ Nag.Inaccuracy
      }
      MateAdvice(sequence, nag, info, prev)
    }
  }
}
