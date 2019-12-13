package lila.report

import com.softwaremill.macwire._
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class ReportApi(
    val coll: Coll,
    userRepo: UserRepo,
    autoAnalysis: AutoAnalysis,
    securityApi: lila.security.SecurityApi,
    userSpyApi: lila.security.UserSpyApi,
    playbanApi: lila.playban.PlaybanApi,
    slackApi: lila.slack.SlackApi,
    isOnline: lila.socket.IsOnline,
    asyncCache: lila.memo.AsyncCache.Builder,
    thresholds: Thresholds
) {

  import BSONHandlers._
  import Report.Candidate

  private lazy val accuracyOf = accuracy.of _

  private lazy val scorer = wire[ReportScore]

  def create(c: Candidate, score: Report.Score => Report.Score = identity): Funit =
    (!c.reporter.user.reportban && !isAlreadySlain(c)) ?? {
      scorer(c) map (_ withScore score) flatMap {
        case scored @ Candidate.Scored(candidate, _) =>
          coll
            .one[Report](
              $doc(
                "user"   -> candidate.suspect.user.id,
                "reason" -> candidate.reason,
                "open"   -> true
              )
            )
            .flatMap { prev =>
              val report = Report.make(scored, prev)
              lila.mon.mod.report.create(report.reason.key).increment()
              if (report.isRecentComm &&
                  report.score.value >= thresholds.slack() &&
                  prev.exists(_.score.value < thresholds.slack())) slackApi.commReportBurst(c.suspect.user)
              coll.update.one($id(report.id), report, upsert = true).void >>
                autoAnalysis(candidate)
            } >>- monitorOpen
      }
    }

  def commFlag(reporter: Reporter, suspect: Suspect, resource: String, text: String) =
    create(
      Candidate(
        reporter,
        suspect,
        Reason.Comm,
        s"${Reason.Comm.flagText} $resource ${text take 140}"
      )
    )

  private def monitorOpen() = {
    nbOpenCache.refresh
    nbOpen foreach { nb =>
      lila.mon.mod.report.unprocessed.increment(nb)
    }
  }

  private def isAlreadySlain(candidate: Candidate) =
    (candidate.isCheat && candidate.suspect.user.engine) ||
      (candidate.isAutomatic && candidate.isOther && candidate.suspect.user.troll) ||
      (candidate.isComm && candidate.suspect.user.troll)

  def getMod(username: String): Fu[Option[Mod]] =
    userRepo named username map2 Mod.apply

  def getLichessMod: Fu[Mod] = userRepo.lichess map2 Mod.apply orFail "User lichess is missing"
  def getLichessReporter: Fu[Reporter] = getLichessMod map { l =>
    Reporter(l.user)
  }

  def getSuspect(username: String): Fu[Option[Suspect]] =
    userRepo named username map2 Suspect.apply

  def autoCheatPrintReport(userId: String): Funit =
    coll.exists(
      $doc(
        "user"   -> userId,
        "reason" -> Reason.CheatPrint.key
      )
    ) flatMap {
      case true => funit // only report once
      case _ =>
        getSuspect(userId) zip getLichessReporter flatMap {
          case (Some(suspect), reporter) =>
            create(
              Candidate(
                reporter = reporter,
                suspect = suspect,
                reason = Reason.CheatPrint,
                text = "Shares print with known cheaters"
              )
            )
          case _ => funit
        }
    }

  def autoCheatReport(userId: String, text: String): Funit =
    getSuspect(userId) zip
      getLichessReporter zip
      findRecent(1, selectRecent(SuspectId(userId), Reason.Cheat)).map(_.flatMap(_.atoms.toList)) flatMap {
      case Some(suspect) ~ reporter ~ atoms if atoms.forall(_.byHuman) =>
        lila.mon.cheat.autoReport.count.increment()
        create(
          Candidate(
            reporter = reporter,
            suspect = suspect,
            reason = Reason.Cheat,
            text = text
          )
        )
      case _ => funit
    }

  def autoBotReport(userId: String, referer: Option[String], name: String): Funit =
    getSuspect(userId) zip getLichessReporter flatMap {
      case (Some(suspect), reporter) =>
        create(
          Candidate(
            reporter = reporter,
            suspect = suspect,
            reason = Reason.Cheat,
            text = s"""$name bot detected on ${referer | "?"}"""
          )
        )
      case _ => funit
    }

  def maybeAutoPlaybanReport(userId: String): Funit =
    userSpyApi.getUserIdsWithSameIpAndPrint(userId) map { ids =>
      playbanApi.bans(ids.toList ::: List(userId)) map { bans =>
        (bans.values.sum >= 80) ?? {
          userRepo.byId(userId) zip
            getLichessReporter zip
            findRecent(1, selectRecent(SuspectId(userId), Reason.Playbans)) flatMap {
            case Some(abuser) ~ reporter ~ past if past.size < 1 =>
              create(
                Candidate(
                  reporter = reporter,
                  suspect = Suspect(abuser),
                  reason = Reason.Playbans,
                  text = s"${bans.values.sum} playbans over ${bans.keys.size} accounts with IP+Print match."
                )
              )
            case _ => funit
          }
        }
      }
    }

  def processAndGetBySuspect(suspect: Suspect): Fu[List[Report]] =
    for {
      all <- recent(suspect, 10)
      open = all.filter(_.open)
      _ <- doProcessReport($inIds(open.map(_.id)), ModId.lichess)
    } yield open

  def autoBoostReport(winnerId: User.ID, loserId: User.ID): Funit =
    securityApi.shareIpOrPrint(winnerId, loserId) zip
      userRepo.byId(winnerId) zip userRepo.byId(loserId) zip getLichessReporter flatMap {
      case isSame ~ Some(winner) ~ Some(loser) ~ reporter =>
        create(
          Candidate(
            reporter = reporter,
            suspect = Suspect(if (isSame) winner else loser),
            reason = Reason.Boost,
            text =
              if (isSame) s"Farms rating points from @${loser.username} (same IP or print)"
              else s"Sandbagging - the winning player @${winner.username} has different IPs & prints"
          )
        )
      case _ => funit
    }

  def process(mod: Mod, reportId: Report.ID): Funit =
    for {
      report  <- coll.byId[Report](reportId) orFail s"no such report $reportId"
      suspect <- getSuspect(report.user) orFail s"No such suspect $report"
      rooms = Set(Room(report.reason))
      res <- process(mod, suspect, rooms, reportId.some)
    } yield res

  def process(mod: Mod, sus: Suspect, rooms: Set[Room], reportId: Option[Report.ID] = None): Funit =
    inquiries.ofModId(mod.user.id) map (_.filter(_.user == sus.user.id)) flatMap { inquiry =>
      val relatedSelector = $doc(
        "user" -> sus.user.id,
        "room" $in rooms,
        "open" -> true
      )
      val reportSelector = reportId.orElse(inquiry.map(_.id)).fold(relatedSelector) { id =>
        $or($id(id), relatedSelector)
      }
      accuracy.invalidate(reportSelector) >>
        doProcessReport(reportSelector, mod.id).void >>- {
        monitorOpen
        lila.mon.mod.report.close.increment()
      }
    }

  private def doProcessReport(selector: Bdoc, by: ModId) = coll.update.one(
    selector,
    $set(
      "open"        -> false,
      "processedBy" -> by.value
    ) ++ $unset("inquiry"),
    multi = true
  )

  def autoInsultReport(userId: String, text: String, major: Boolean): Funit = {
    getSuspect(userId) zip getLichessReporter flatMap {
      case (Some(suspect), reporter) =>
        create(
          Candidate(
            reporter = reporter,
            suspect = suspect,
            reason = Reason.Comm,
            text = text
          ),
          score => if (major) Report.Score(score.value atLeast thresholds.score()) else score
        )
      case _ => funit
    }
  } >>- monitorOpen

  def moveToXfiles(id: String): Funit =
    coll.update
      .one(
        $id(id),
        $set("room" -> Room.Xfiles.key) ++ $unset("inquiry")
      )
      .void

  private val closedSelect: Bdoc   = $doc("open" -> false)
  private def scoreThresholdSelect = $doc("score" $gte thresholds.score())
  private val sortLastAtomAt       = $doc("atoms.0.at" -> -1)

  private def roomSelect(room: Option[Room]): Bdoc =
    room.fold($doc("room" $ne Room.Xfiles.key)) { r =>
      $doc("room" -> r)
    }

  private def selectOpenAvailableInRoom(room: Option[Room]) =
    $doc("open" -> true, "inquiry" $exists false) ++ roomSelect(room) ++ scoreThresholdSelect

  val nbOpenCache = asyncCache.single[Int](
    name = "report.nbOpen",
    f = coll.countSel(selectOpenAvailableInRoom(none)),
    expireAfter = _.ExpireAfterWrite(1 hour)
  )
  def nbOpen = nbOpenCache.get

  def recent(
      suspect: Suspect,
      nb: Int,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): Fu[List[Report]] =
    coll.ext.find($doc("user" -> suspect.id.value)).sort(sortLastAtomAt).list[Report](nb, readPreference)

  def moreLike(report: Report, nb: Int): Fu[List[Report]] =
    coll.ext.find($doc("user" -> report.user, "_id" $ne report.id)).sort(sortLastAtomAt).list[Report](nb)

  def byAndAbout(user: User, nb: Int): Fu[Report.ByAndAbout] =
    for {
      by <- coll.ext
        .find(
          $doc("atoms.by" -> user.id)
        )
        .sort(sortLastAtomAt)
        .list[Report](nb, ReadPreference.secondaryPreferred)
      about <- recent(Suspect(user), nb, ReadPreference.secondaryPreferred)
    } yield Report.ByAndAbout(by, about)

  def currentCheatScore(suspect: Suspect): Fu[Option[Report.Score]] =
    coll.primitiveOne[Report.Score](
      $doc(
        "user" -> suspect.user.id,
        "room" -> Room.Cheat.key,
        "open" -> true
      ),
      "score"
    )

  def currentCheatReport(suspect: Suspect): Fu[Option[Report]] =
    coll.one[Report](
      $doc(
        "user" -> suspect.user.id,
        "room" -> Room.Cheat.key,
        "open" -> true
      )
    )

  def recentReportersOf(sus: Suspect): Fu[List[ReporterId]] =
    coll.secondaryPreferred.distinctEasy[ReporterId, List](
      "atoms.by",
      $doc(
        "user" -> sus.user.id,
        "atoms.0.at" $gt DateTime.now.minusDays(3)
      )
    ) dmap (_ filterNot ReporterId.lichess.==)

  def openAndRecentWithFilter(nb: Int, room: Option[Room]): Fu[List[Report.WithSuspect]] =
    for {
      opens <- findBest(nb, selectOpenAvailableInRoom(room))
      nbClosed = nb - opens.size
      closed <- if (room.has(Room.Xfiles) || nbClosed < 1) fuccess(Nil)
      else findRecent(nbClosed, closedSelect ++ roomSelect(room))
      withNotes <- addSuspectsAndNotes(opens ++ closed)
    } yield withNotes

  def next(room: Room): Fu[Option[Report]] =
    findBest(1, selectOpenAvailableInRoom(room.some)).map(_.headOption)

  private def addSuspectsAndNotes(reports: List[Report]): Fu[List[Report.WithSuspect]] =
    userRepo byIdsSecondary (reports.map(_.user).distinct) map { users =>
      reports
        .flatMap { r =>
          users.find(_.id == r.user) map { u =>
            Report.WithSuspect(r, Suspect(u), isOnline(u.id))
          }
        }
        .sortBy(-_.urgency)
    }

  object accuracy {

    private val cache = asyncCache.clearable[User.ID, Option[Int]](
      name = "reporterAccuracy",
      f = a => forUser(a).map2((a: Accuracy) => a.value),
      expireAfter = _.ExpireAfterWrite(24 hours)
    )

    private def forUser(reporterId: User.ID): Fu[Option[Accuracy]] =
      coll.ext
        .find(
          $doc(
            "atoms.by" -> reporterId,
            "room"     -> Room.Cheat.key,
            "open"     -> false
          )
        )
        .sort(sortLastAtomAt)
        .list[Report](20, ReadPreference.secondaryPreferred) flatMap { reports =>
        if (reports.size < 4) fuccess(none) // not enough data to know
        else {
          val userIds = reports.map(_.user).distinct
          userRepo countEngines userIds map { nbEngines =>
            Accuracy {
              Math.round((nbEngines + 0.5f) / (userIds.length + 2f) * 100)
            }.some
          }
        }
      }

    def of(reporter: ReporterId): Fu[Option[Accuracy]] =
      cache get reporter.value map2 Accuracy.apply

    def apply(candidate: Candidate): Fu[Option[Accuracy]] =
      (candidate.reason == Reason.Cheat) ?? of(candidate.reporter.id)

    def invalidate(selector: Bdoc): Funit =
      coll
        .distinctEasy[User.ID, List]("atoms.by", selector)
        .map {
          _ foreach cache.invalidate
        }
        .void
  }

  def countOpenByRooms: Fu[Room.Counts] =
    coll
      .aggregateList(maxDocs = 100) { framework =>
        import framework._
        Match(selectOpenAvailableInRoom(none)) -> List(
          GroupField("room")("nb" -> SumAll)
        )
      }
      .map { docs =>
        Room.Counts(docs.flatMap { doc =>
          doc.string("_id") flatMap Room.apply flatMap { room =>
            doc.int("nb") map { room -> _ }
          }
        }.toMap)
      }

  private def findRecent(nb: Int, selector: Bdoc): Fu[List[Report]] = (nb > 0) ?? {
    coll.ext.find(selector).sort(sortLastAtomAt).list[Report](nb)
  }

  private def findBest(nb: Int, selector: Bdoc): Fu[List[Report]] = (nb > 0) ?? {
    coll.ext.find(selector).sort($sort desc "score").list[Report](nb)
  }

  private def selectRecent(suspect: SuspectId, reason: Reason): Bdoc = $doc(
    "atoms.0.at" $gt DateTime.now.minusDays(7),
    "user"   -> suspect.value,
    "reason" -> reason
  )

  object inquiries {

    def all: Fu[List[Report]] = coll.list[Report]($doc("inquiry.mod" $exists true))

    def ofModId(modId: User.ID): Fu[Option[Report]] = coll.one[Report]($doc("inquiry.mod" -> modId))

    /*
     * If the mod has no current inquiry, just start this one.
     * If they had another inquiry, cancel it and start this one instead.
     * If they already are on this inquiry, cancel it.
     */
    def toggle(mod: Mod, id: Report.ID): Fu[Option[Report]] =
      for {
        report  <- coll.byId[Report](id) orFail s"No report $id found"
        current <- ofModId(mod.user.id)
        _       <- current ?? cancel(mod)
        isSame = current.exists(_.id == report.id)
        _ <- !isSame ?? coll
          .updateField(
            $id(report.id),
            "inquiry",
            Report.Inquiry(mod.user.id, DateTime.now)
          )
          .void
      } yield !isSame option report

    def cancel(mod: Mod)(report: Report): Funit =
      if (report.isOther && report.onlyAtom.map(_.by.value).has(mod.user.id))
        coll.delete.one($id(report.id)).void // cancel spontaneous inquiry
      else
        coll.update
          .one(
            $id(report.id),
            $unset("inquiry", "processedBy") ++ $set("open" -> true)
          )
          .void

    def spontaneous(mod: Mod, sus: Suspect): Fu[Report] = ofModId(mod.user.id) flatMap { current =>
      current.??(cancel(mod)) >> {
        val report = Report
          .make(
            Candidate(
              Reporter(mod.user),
              sus,
              Reason.Other,
              Report.spontaneousText
            ) scored Report.Score(0),
            none
          )
          .copy(inquiry = Report.Inquiry(mod.user.id, DateTime.now).some)
        coll.insert.one(report) inject report
      }
    }

    private[report] def expire: Funit = {
      val selector = $doc(
        "inquiry.mod" $exists true,
        "inquiry.seenAt" $lt DateTime.now.minusMinutes(20)
      )
      coll.delete.one(selector ++ $doc("text" -> Report.spontaneousText)) >>
        coll.update.one(selector, $unset("inquiry"), multi = true).void
    }
  }
}
