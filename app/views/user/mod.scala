package views.html.user

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.evaluation.Display
import lila.security.{ FingerHash, Permission }
import lila.playban.RageSit
import lila.user.User

import controllers.routes

object mod {

  def menu(u: User)(implicit ctx: Context) = div(id := "mz_menu")(
    div(cls := "inner")(
      a(href := "#mz_actions")("Actions"),
      canViewRoles(u) option a(href := "#mz_roles")("Roles"),
      a(href := "#mz_irwin")("Irwin"),
      a(href := "#mz_assessments")("Evaluation"),
      a(href := "#mz_plan", cls := "mz_plan")("Patron"),
      a(href := "#mz_mod_log")("Mod log"),
      a(href := "#mz_reports_out")("Reports sent"),
      a(href := "#mz_reports_in")("Reports received"),
      a(href := "#mz_others")("Accounts"),
      a(href := "#mz_identification")("Identification"),
      a(href := "#us_profile")("Profile")
    )
  )

  def actions(u: User, emails: User.Emails, erased: User.Erased)(implicit ctx: Context): Frag =
    div(id := "mz_actions")(
      isGranted(_.UserEvaluate) option div(cls := "btn-rack")(
        postForm(action := routes.Mod.spontaneousInquiry(u.username), title := "Start an inquiry")(
          submitButton(cls := "btn-rack__btn inquiry")(i)
        ),
        postForm(
          action := routes.Mod.refreshUserAssess(u.username),
          title := "Collect data and ask irwin",
          cls := "xhr"
        )(
          submitButton(cls := "btn-rack__btn")("Evaluate")
        ),
        isGranted(_.Shadowban) option {
          a(
            cls := "btn-rack__btn",
            href := routes.Mod.communicationPublic(u.id),
            title := "View communications"
          )("Comms")
        },
        postForm(action := routes.Mod.notifySlack(u.id), title := "Notify #tavern", cls := "xhr")(
          submitButton(cls := "btn-rack__btn")("Slack")
        )
      ),
      div(cls := "btn-rack")(
        isGranted(_.CloseAccount) option {
          postForm(
            action := routes.Mod.alt(u.username, !u.marks.alt),
            title := "Preemptively close unauthorized alt.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.alt))("Alt")
          )
        },
        isGranted(_.MarkEngine) option {
          postForm(
            action := routes.Mod.engine(u.username, !u.marks.engine),
            title := "This user is clearly cheating.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.engine))("Engine")
          )
        },
        isGranted(_.MarkBooster) option {
          postForm(
            action := routes.Mod.booster(u.username, !u.marks.boost),
            title := "Marks the user as a booster or sandbagger.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.boost))("Booster")
          )
        },
        isGranted(_.Shadowban) option {
          postForm(
            action := routes.Mod.troll(u.username, !u.marks.troll),
            title := "Enable/disable communication features for this user.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.troll))("Shadowban")
          )
        },
        u.marks.troll option {
          postForm(
            action := routes.Mod.deletePmsAndChats(u.username),
            title := "Delete all PMs and public chat messages",
            cls := "xhr"
          )(
            submitButton(cls := "btn-rack__btn confirm")("Clear PMs & chats")
          )
        },
        isGranted(_.RemoveRanking) option {
          postForm(
            action := routes.Mod.rankban(u.username, !u.marks.rankban),
            title := "Include/exclude this user from the rankings.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.rankban))("Rankban")
          )
        },
        isGranted(_.ReportBan) option {
          postForm(
            action := routes.Mod.reportban(u.username, !u.marks.reportban),
            title := "Enable/disable the report feature for this user.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.reportban))("Reportban")
          )
        }
      ),
      div(cls := "btn-rack")(
        isGranted(_.IpBan) option {
          postForm(action := routes.Mod.ipBan(u.username, !u.marks.ipban), cls := "xhr")(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.ipban))("IP ban")
          )
        },
        if (u.enabled) {
          isGranted(_.CloseAccount) option {
            postForm(
              action := routes.Mod.closeAccount(u.username),
              title := "Disables this account.",
              cls := "xhr"
            )(
              submitButton(cls := "btn-rack__btn")("Close")
            )
          }
        } else if (erased.value) {
          "Erased"
        } else {
          isGranted(_.CloseAccount) option {
            postForm(
              action := routes.Mod.reopenAccount(u.username),
              title := "Re-activates this account.",
              cls := "xhr"
            )(
              submitButton(cls := "btn-rack__btn active")("Closed")
            )
          }
        }
      ),
      div(cls := "btn-rack")(
        (u.totpSecret.isDefined && isGranted(_.DisableTwoFactor)) option {
          postForm(
            action := routes.Mod.disableTwoFactor(u.username),
            title := "Disables two-factor authentication for this account.",
            cls := "xhr"
          )(
            submitButton(cls := "btn-rack__btn confirm")("Disable 2FA")
          )
        },
        (isGranted(_.Impersonate) || (isGranted(_.Admin) && u.id == "lichess")) option {
          postForm(action := routes.Mod.impersonate(u.username))(
            submitButton(cls := "btn-rack__btn")("Impersonate")
          )
        }
      ),
      isGranted(_.SetTitle) option {
        postForm(cls := "fide_title", action := routes.Mod.setTitle(u.username))(
          form3.select(
            lila.user.DataForm.title.fill(u.title.map(_.value))("title"),
            lila.user.Title.all,
            "No title".some
          )
        )
      },
      (isGranted(_.Doxing) && isGranted(_.SetEmail)) ?? frag(
        postForm(cls := "email", action := routes.Mod.setEmail(u.username))(
          st.input(
            tpe := "email",
            value := emails.current.??(_.value),
            name := "email",
            placeholder := "Email address"
          ),
          submitButton(cls := "button", dataIcon := "E")
        ),
        emails.previous.map { email =>
          s"Previously $email"
        }
      )
    )

  def parts(
      u: User,
      history: List[lila.mod.Modlog],
      charges: List[lila.plan.Charge],
      reports: lila.report.Report.ByAndAbout,
      pref: lila.pref.Pref,
      rageSit: RageSit
  )(implicit ctx: Context) = frag(
    roles(u),
    prefs(pref),
    plan(charges),
    showRageSit(rageSit),
    modLog(history),
    reportLog(u, reports)
  )

  def roles(u: User)(implicit ctx: Context) = canViewRoles(u) option div(cls := "mz_roles")(
    (if (isGranted(_.ChangePermission)) a(href := routes.Mod.permissions(u.username)) else span)(
      strong(cls := "text inline", dataIcon := " ")("Mod permissions: "),
      if (u.roles.isEmpty) "Add some" else Permission(u.roles).map(_.name).mkString(", ")
    )
  )

  def prefs(pref: lila.pref.Pref) = div(id := "mz_preferences")(
    strong(cls := "text inline", dataIcon := "%")("Notable preferences:"),
    ul(
      (pref.keyboardMove != lila.pref.Pref.KeyboardMove.NO) option li("keyboard moves"),
      pref.botCompatible option li(
        strong(
          a(
            cls := "text",
            dataIcon := "j",
            href := lila.common.String.base64
              .decode("aHR0cDovL2NoZXNzLWNoZWF0LmNvbS9ob3dfdG9fY2hlYXRfYXRfbGljaGVzcy5odG1s")
          )("BOT-COMPATIBLE SETTINGS")
        )
      )
    )
  )

  def showRageSit(rageSit: RageSit) = div(id := "mz_sitdccounter")(
    strong(cls := "text inline")("Ragesit counter: "),
    span(cls := "text inline")(rageSit.counterView)
  )

  def plan(charges: List[lila.plan.Charge])(implicit ctx: Context) = charges.headOption.map { firstCharge =>
    div(id := "mz_plan")(
      strong(cls := "text", dataIcon := patronIconChar)(
        "Patron payments",
        isGranted(_.PayPal) option {
          firstCharge.payPal.flatMap(_.subId).map { subId =>
            frag(
              " - ",
              a(
                href := s"https://www.paypal.com/fr/cgi-bin/webscr?cmd=_profile-recurring-payments&encrypted_profile_id=$subId"
              )("[PayPal sub]")
            )
          }
        }
      ),
      ul(
        charges.map { c =>
          li(c.cents.usd.toString, " with ", c.serviceName, " on ", absClientDateTime(c.date))
        }
      ),
      br
    )
  }

  def modLog(history: List[lila.mod.Modlog])(implicit lang: Lang) = div(id := "mz_mod_log")(
    strong(cls := "text", dataIcon := "!")("Moderation history", history.isEmpty option ": nothing to show"),
    history.nonEmpty ?? frag(
      ul(
        history.map { e =>
          li(
            userIdLink(e.mod.some, withTitle = false),
            " ",
            b(e.showAction),
            " ",
            e.details,
            " ",
            momentFromNowOnce(e.date)
          )
        }
      ),
      br
    )
  )

  def reportLog(u: User, reports: lila.report.Report.ByAndAbout)(implicit lang: Lang) = frag(
    div(id := "mz_reports_out", cls := "mz_reports")(
      strong(cls := "text", dataIcon := "!")(
        s"Reports sent by ${u.username}",
        reports.by.isEmpty option ": nothing to show."
      ),
      reports.by.map { r =>
        r.atomBy(lila.report.ReporterId(u.id)).map { atom =>
          postForm(action := routes.Report.inquiry(r.id))(
            submitButton(reportScore(r.score), " ", strong(r.reason.name)),
            " ",
            userIdLink(r.user.some),
            " ",
            momentFromNowOnce(atom.at),
            ": ",
            shorten(atom.text, 200)
          )
        }
      }
    ),
    div(id := "mz_reports_in", cls := "mz_reports")(
      strong(cls := "text", dataIcon := "!")(
        s"Reports concerning ${u.username}",
        reports.about.isEmpty option ": nothing to show."
      ),
      reports.about.map { r =>
        postForm(action := routes.Report.inquiry(r.id))(
          submitButton(reportScore(r.score), " ", strong(r.reason.name)),
          div(cls := "atoms")(
            r.bestAtoms(3).map { atom =>
              div(cls := "atom")(
                "By ",
                userIdLink(atom.by.value.some),
                " ",
                momentFromNowOnce(atom.at),
                ": ",
                shorten(atom.text, 200)
              )
            },
            (r.atoms.size > 3) option s"(and ${r.atoms.size - 3} more)"
          )
        )
      }
    )
  )

  def assessments(pag: lila.evaluation.PlayerAggregateAssessment.WithGames)(implicit ctx: Context): Frag =
    div(id := "mz_assessments")(
      pag.pag.sfAvgBlurs.map { blursYes =>
        p(cls := "text", dataIcon := "j")(
          "ACPL in games with blurs is ",
          strong(blursYes._1),
          " [",
          blursYes._2,
          " , ",
          blursYes._3,
          "]",
          pag.pag.sfAvgNoBlurs ?? { blursNo =>
            frag(
              " against ",
              strong(blursNo._1),
              " [",
              blursNo._2,
              ", ",
              blursNo._3,
              "] in games without blurs."
            )
          }
        )
      },
      pag.pag.sfAvgLowVar.map { lowVar =>
        p(cls := "text", dataIcon := "j")(
          "ACPL in games with consistent move times is ",
          strong(lowVar._1),
          " [",
          lowVar._2,
          ", ",
          lowVar._3,
          "]",
          pag.pag.sfAvgHighVar ?? { highVar =>
            frag(
              " against ",
              strong(highVar._1),
              " [",
              highVar._2,
              ", ",
              highVar._3,
              "] in games with random move times."
            )
          }
        )
      },
      pag.pag.sfAvgHold.map { holdYes =>
        p(cls := "text", dataIcon := "j")(
          "ACPL in games with bot signature ",
          strong(holdYes._1),
          " [",
          holdYes._2,
          ", ",
          holdYes._3,
          "]",
          pag.pag.sfAvgNoHold ?? { holdNo =>
            frag(
              " against ",
              strong(holdNo._1),
              " [",
              holdNo._2,
              ", ",
              holdNo._3,
              "]  in games without bot signature."
            )
          }
        )
      },
      table(cls := "slist")(
        thead(
          tr(
            th("Opponent"),
            th("Game"),
            th("Centi-Pawn", br, "(Avg ± SD)"),
            th("Move Times", br, "(Avg ± SD)"),
            th(span(title := "The frequency of which the user leaves the game page.")("Blurs")),
            th(span(title := "Bot detection using grid click analysis.")("Bot")),
            th(span(title := "Aggregate match")(raw("&Sigma;")))
          )
        ),
        tbody(
          pag.pag.playerAssessments
            .sortBy(-_.assessment.id)
            .take(15)
            .map { result =>
              tr(
                td(
                  a(href := routes.Round.watcher(result.gameId, result.color.name))(
                    pag.pov(result) match {
                      case None    => result.gameId
                      case Some(p) => playerUsername(p.opponent)
                    }
                  )
                ),
                td(
                  pag.pov(result).map { p =>
                    a(href := routes.Round.watcher(p.gameId, p.color.name))(
                      p.game.isTournament option iconTag("g"),
                      p.game.perfType.map { pt =>
                        iconTag(pt.iconChar)
                      },
                      shortClockName(p.game.clock.map(_.config))
                    )
                  }
                ),
                td(
                  span(cls := s"sig sig_${Display.stockfishSig(result)}", dataIcon := "J"),
                  s" ${result.sfAvg} ± ${result.sfSd}"
                ),
                td(
                  span(cls := s"sig sig_${Display.moveTimeSig(result)}", dataIcon := "J"),
                  s" ${result.mtAvg / 10} ± ${result.mtSd / 10}",
                  (~result.mtStreak) ?? frag(br, "STREAK")
                ),
                td(
                  span(cls := s"sig sig_${Display.blurSig(result)}", dataIcon := "J"),
                  s" ${result.blurs}%",
                  result.blurStreak.filter(8 <=) map { s =>
                    frag(br, s"STREAK $s/12")
                  }
                ),
                td(
                  span(cls := s"sig sig_${Display.holdSig(result)}", dataIcon := "J"),
                  if (result.hold) "Yes" else "No"
                ),
                td(
                  div(cls := "aggregate")(
                    span(cls := s"sig sig_${result.assessment.id}")(result.assessment.emoticon)
                  )
                )
              )
            }
        )
      )
    )

  private val sortNumberTh = th(attr("data-sort-method") := "number")
  private val dataSort     = attr("data-sort")
  private val playban      = iconTag("p")
  private val alt          = raw("A")
  private val shadowban    = iconTag("c")
  private val boosting     = iconTag("9")
  private val engine       = iconTag("n")
  private val ipban        = iconTag("2")
  private val closed       = iconTag("k")
  private val reportban    = iconTag("!")
  private val notesText    = iconTag("m")
  private def markTd(nb: Int, content: => Frag) =
    if (nb > 0) td(cls := "i", dataSort := nb)(content)
    else td

  def otherUsers(
      u: User,
      spy: lila.security.UserSpy,
      othersWithEmail: lila.security.UserSpy.WithMeSortedWithEmails,
      notes: List[lila.user.Note],
      bans: Map[String, Int]
  )(implicit ctx: Context): Frag =
    div(id := "mz_others")(
      table(cls := "slist")(
        thead(
          tr(
            th(spy.otherUsers.size, " similar user(s)"),
            th("Email"),
            sortNumberTh("Same"),
            th("Games"),
            sortNumberTh(playban)(cls := "i", title := "Playban"),
            sortNumberTh(alt)(cls := "i", title := "Alt"),
            sortNumberTh(shadowban)(cls := "i", title := "Shadowban"),
            sortNumberTh(boosting)(cls := "i", title := "Boosting"),
            sortNumberTh(engine)(cls := "i", title := "Engine"),
            sortNumberTh(ipban)(cls := "i", title := "IP ban"),
            sortNumberTh(closed)(cls := "i", title := "Closed"),
            sortNumberTh(reportban)(cls := "i", title := "Reportban"),
            sortNumberTh(notesText)(cls := "i", title := "Notes"),
            sortNumberTh("Created"),
            sortNumberTh("Active")
          )
        ),
        tbody(
          othersWithEmail.others.map {
            case lila.security.UserSpy.OtherUser(o, byIp, byFp) =>
              val dox = isGranted(_.Doxing) || (o.lameOrAlt && !o.hasTitle)
              val userNotes =
                notes.filter(n => n.to == o.id && (ctx.me.exists(n.isFrom) || isGranted(_.Doxing)))
              tr(o == u option (cls := "same"))(
                if (dox || o == u) td(dataSort := o.id)(userLink(o, withBestRating = true, params = "?mod"))
                else td,
                if (dox) td(othersWithEmail emailValueOf o)
                else td,
                td(dataSort := (byIp ?? 3) + (byFp ?? 1))(
                  if (o == u) "-"
                  else List(byIp option "IP", byFp option "Print").flatten.mkString(", ")
                ),
                td(dataSort := o.count.game)(o.count.game.localize),
                markTd(~bans.get(o.id), playban(cls := "text")(~bans.get(o.id))),
                markTd(o.marks.alt ?? 1, alt),
                markTd(o.marks.troll ?? 1, shadowban),
                markTd(o.marks.boost ?? 1, boosting),
                markTd(o.marks.engine ?? 1, engine),
                markTd(o.marks.ipban ?? 1, ipban(cls := "is-red")),
                markTd(o.disabled ?? 1, closed),
                markTd(o.marks.reportban ?? 1, reportban),
                userNotes.nonEmpty option {
                  td(dataSort := userNotes.size)(
                    a(href := s"${routes.User.show(o.username)}?notes")(
                      notesText(
                        title := s"Notes from ${userNotes.map(_.from).map(usernameOrId).mkString(", ")}",
                        cls := "is-green"
                      ),
                      userNotes.size
                    )
                  )
                } getOrElse td(dataSort := 0),
                td(dataSort := o.createdAt.getMillis)(momentFromNowOnce(o.createdAt)),
                td(dataSort := o.seenAt.map(_.getMillis.toString))(o.seenAt.map(momentFromNowOnce))
              )
          }
        )
      )
    )

  def identification(spy: lila.security.UserSpy, printBlock: FingerHash => Boolean): Frag =
    div(id := "mz_identification")(
      div(cls := "spy_ips")(
        strong(spy.ips.size, " IP addresses"),
        ul(
          spy.ipsByLocations.map {
            case (location, ips) => {
              li(
                p(location.toString),
                ul(
                  ips.map { ip =>
                    li(cls := "ip")(
                      a(
                        cls := List("address" -> true, "blocked" -> ip.blocked),
                        href := s"${routes.Mod.search}?q=${ip.ip.value}"
                      )(
                        tag("ip")(ip.ip.value.value),
                        " ",
                        momentFromNowOnce(ip.ip.date)
                      )
                    )
                  }
                )
              )
            }
          }
        )
      ),
      div(cls := "spy_uas")(
        strong(spy.uas.size, " User agent(s)"),
        ul(
          spy.uas.sorted.map { ua =>
            li(ua.value, " ", momentFromNowOnce(ua.date))
          }
        )
      ),
      div(cls := "spy_fps")(
        strong(pluralize("Fingerprint", spy.prints.size)),
        ul(
          spy.prints.sorted.map { fp =>
            li(
              a(href := routes.Mod.print(fp.value.value), cls := printBlock(fp.value) option "blocked")(
                fp.value.value,
                " ",
                momentFromNowOnce(fp.date)
              )
            )
          }
        )
      )
    )

  def userMarks(o: User, playbans: Option[Int]) = div(cls := "user_marks")(
    playbans.map { nb =>
      playban(nb)
    },
    o.marks.troll option shadowban,
    o.marks.boost option boosting,
    o.marks.engine option engine,
    o.marks.ipban option ipban,
    o.disabled option closed,
    o.marks.reportban option reportban
  )
}
