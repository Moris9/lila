package lila.pref

import play.api.data._
import play.api.data.Forms._

import lila.user.User

private[pref] final class DataForm(api: PrefApi) {

  val pref = Form(mapping(
    "autoQueen" -> number.verifying(Pref.AutoQueen.choices.toMap contains _),
    "autoThreefold" -> number.verifying(Pref.AutoThreefold.choices.toMap contains _),
    "takeback" -> number.verifying(Pref.Takeback.choices.toMap contains _),
    "clockTenths" -> number.verifying(Set(0, 1) contains _),
    "clockBar" -> number.verifying(Set(0, 1) contains _),
    "follow" -> number.verifying(Set(0, 1) contains _),
    "highlight" -> number.verifying(Set(0, 1) contains _),
    "destination" -> number.verifying(Set(0, 1) contains _),
    "challenge" -> number.verifying(Pref.Challenge.choices.toMap contains _),
    "premove" -> number.verifying(Set(0, 1) contains _),
    "animation" -> number.verifying(Set(0, 1, 2, 3) contains _),
    "captured" -> number.verifying(Set(0, 1) contains _)
  )(PrefData.apply)(PrefData.unapply))

  case class PrefData(
      autoQueen: Int,
      autoThreefold: Int,
      takeback: Int,
      clockTenths: Int,
      clockBar: Int,
      follow: Int,
      highlight: Int,
      destination: Int,
      challenge: Int,
      premove: Int,
      animation: Int,
      captured: Int) {

    def apply(pref: Pref) = pref.copy(
      autoQueen = autoQueen,
      autoThreefold = autoThreefold,
      takeback = takeback,
      clockTenths = clockTenths == 1,
      clockBar = clockBar == 1,
      follow = follow == 1,
      highlight = highlight == 1,
      destination = destination == 1,
      challenge = challenge,
      premove = premove == 1,
      animation = animation,
      captured = captured == 1)
  }

  object PrefData {

    def apply(pref: Pref): PrefData = PrefData(
      autoQueen = pref.autoQueen,
      autoThreefold = pref.autoThreefold,
      takeback = pref.takeback,
      clockTenths = pref.clockTenths.fold(1, 0),
      clockBar = pref.clockBar.fold(1, 0),
      follow = pref.follow.fold(1, 0),
      highlight = pref.highlight.fold(1, 0),
      destination = pref.destination.fold(1, 0),
      challenge = pref.challenge,
      premove = pref.premove.fold(1, 0),
      animation = pref.animation,
      captured = pref.captured.fold(1, 0))
  }

  def prefOf(user: User): Fu[Form[PrefData]] = api getPref user map { p =>
    pref fill PrefData(p)
  }

  val theme = Form(single(
    "theme" -> nonEmptyText.verifying(Theme contains _)
  ))

  val pieceSet = Form(single(
    "set" -> nonEmptyText.verifying(PieceSet contains _)
  ))

  val bg = Form(single(
    "bg" -> text.verifying(List("light", "dark") contains _)
  ))
}
