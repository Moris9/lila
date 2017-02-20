package lila.report

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.{ User, Note }

case class Report(
    _id: String, // also the url slug
    user: String, // the reportee
    reason: String,
    text: String,
    processedBy: Option[String],
    createdAt: DateTime,
    createdBy: String
) {

  def id = _id
  def slug = _id

  def isCreator(user: String) = user == createdBy

  def isCheat = realReason == Reason.Cheat
  def isOther = realReason == Reason.Other
  def isTroll = realReason == Reason.Troll
  def isInsult = realReason == Reason.Insult
  def isTrollOrInsult = realReason == Reason.Troll || realReason == Reason.Insult

  def unprocessedCheat = unprocessed && isCheat
  def unprocessedOther = unprocessed && isOther
  def unprocessedTroll = unprocessed && isTroll
  def unprocessedInsult = unprocessed && isInsult
  def unprocessedTrollOrInsult = unprocessed && isTrollOrInsult

  def isCommunication = Reason.communication contains realReason

  def isAutomatic = createdBy == "lichess"
  def isManual = !isAutomatic

  def process(by: User) = copy(processedBy = by.id.some)

  def unprocessed = processedBy.isEmpty
  def processed = processedBy.isDefined

  def userIds = List(user, createdBy)

  lazy val realReason: Reason = Reason byKey reason
}

object Report {

  case class WithUser(report: Report, user: User, isOnline: Boolean) {

    def urgency: Int =
      (nowSeconds - report.createdAt.getSeconds).toInt +
        (isOnline ?? (86400 * 5)) +
        (report.processed ?? Int.MinValue)
  }

  case class WithUserAndNotes(withUser: WithUser, notes: List[Note]) {
    def report = withUser.report
    def user = withUser.user
    def hasLichessNote = notes.exists(_.from == "lichess")
    def hasIrwinNote = notes.exists(_.from == "irwin")

    def userIds = report.userIds ::: notes.flatMap(_.userIds)
  }

  case class ByAndAbout(by: List[Report], about: List[Report]) {
    def userIds = by.flatMap(_.userIds) ::: about.flatMap(_.userIds)
  }

  def make(
    user: User,
    reason: Reason,
    text: String,
    createdBy: User
  ): Report = new Report(
    _id = Random nextString 8,
    user = user.id,
    reason = reason.key,
    text = text,
    processedBy = none,
    createdAt = DateTime.now,
    createdBy = createdBy.id
  )
}
