package lila.report

import play.api.libs.json._

import lila.db.api._
import lila.db.Implicits._
import lila.user.{ User, Evaluator }
import tube.reportTube

final class ReportApi(evaluator: Evaluator) {

  def create(setup: ReportSetup, by: User): Funit =
    Reason(setup.reason).fold[Funit](fufail("Invalid report reason " + setup.reason)) { reason ⇒
      val report = Report.make(
        user = setup.user,
        reason = reason,
        text = setup.text,
        createdBy = by)
      $insert(report) >> {
        (report.isCheat && report.isManual) ?? evaluator.generate(report.user, true).void
      }
    }

  def process(id: String, by: User): Funit =
    $update.field(id, "processedBy", by.id)

  def nbUnprocessed = $count(Json.obj("processedBy" -> $exists(false)))

  def recent = $find($query.all sort $sort.createdDesc, 50)
}
