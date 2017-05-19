package lila.irwin

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._
import lila.game.{ Pov, GameRepo }
import lila.tournament.{ Tournament, RankedPlayer }
import lila.user.User

final class IrwinApi(
    reportColl: Coll,
    requestColl: Coll,
    modApi: lila.mod.ModApi,
    notifyApi: lila.notify.NotifyApi
) {

  import BSONHandlers._

  def status(user: User): Fu[IrwinStatus] =
    reports.withPovs(user) zip requests.get(user.id) map { (IrwinStatus.apply _).tupled }

  def dashboard: Fu[IrwinDashboard] = for {
    queue <- requestColl.find($empty).sort($sort asc "priority").list[IrwinRequest](20)
    recent <- reportColl.find($empty).sort($sort desc "date").list[IrwinReport](20)
  } yield IrwinDashboard(queue, recent)

  object reports {

    def insert(report: IrwinReport) = for {
      _ <- reportColl.update($id(report.id), report, upsert = true)
      request <- requests get report.id
      _ <- request.??(r => requests.drop(r.id))
      _ <- request.??(notifyRequester)
      _ <- actAsMod(report)
    } yield ()

    def get(user: User): Fu[Option[IrwinReport]] =
      reportColl.find($id(user.id)).uno[IrwinReport]

    def withPovs(user: User): Fu[Option[IrwinReport.WithPovs]] = get(user) flatMap {
      _ ?? { report =>
        GameRepo.gamesFromSecondary(report.games.map(_.gameId)) map { games =>
          val povs = games.flatMap { g =>
            Pov(g, user) map { g.id -> _ }
          }.toMap
          IrwinReport.WithPovs(report, povs).some
        }
      }
    }

    private val hasTheSlaveWhip = false

    private def actAsMod(report: IrwinReport): Funit = hasTheSlaveWhip ?? {
      report.isLegit ?? {
        case false => modApi.setEngine("irwin", report.userId, true)
        case _ => funit // don't close report of legit player
      }
    }
  }

  object requests {

    import IrwinRequest.Origin

    def getAndStart: Fu[Option[IrwinRequest]] =
      requestColl
        .find($doc("startedAt" $exists false))
        .sort($sort asc "priority")
        .uno[IrwinRequest] flatMap {
          _ ?? { request =>
            requestColl.updateField($id(request.id), "startedAt", DateTime.now) inject request.some
          }
        }

    def get(reportedId: User.ID): Fu[Option[IrwinRequest]] =
      requestColl.byId[IrwinRequest](reportedId)

    def fromMod(reportedId: User.ID, mod: User) = insert(reportedId, _.Moderator, mod.id.some)

    private[irwin] def drop(reportedId: User.ID): Funit = requestColl.remove($id(reportedId)).void

    private[irwin] def insert(reportedId: User.ID, origin: Origin.type => Origin, notifyUserId: Option[User.ID]) = {
      val request = IrwinRequest.make(reportedId, origin(Origin), notifyUserId)
      get(reportedId) flatMap {
        case Some(prev) if prev.isInProgress => funit
        case Some(prev) if prev.priority isAfter request.priority =>
          requestColl.update($id(request.id), request).void
        case Some(prev) => funit
        case None => requestColl.insert(request).void
      }
    }

    private[irwin] def fromTournamentLeaders(leaders: Map[Tournament, List[RankedPlayer]]): Funit =
      lila.common.Future.applySequentially(leaders.toList) {
        case (tour, rps) =>
          val userIds = rps.filter(_.rank <= tour.nbPlayers * 5 / 100).map(_.player.userId)
          lila.common.Future.applySequentially(userIds) { userId =>
            insert(userId, _.Tournament, none)
          }
      }

    private[irwin] def fromLeaderboard(leaders: List[User.ID]): Funit =
      lila.common.Future.applySequentially(leaders) { userId =>
        insert(userId, _.Leaderboard, none)
      }
  }

  private def notifyRequester(request: IrwinRequest): Funit = request.notifyUserId ?? { userId =>
    import lila.notify.{ Notification, IrwinDone }
    notifyApi.addNotification(
      Notification.make(Notification.Notifies(userId), IrwinDone(request.id))
    )
  }
}
