package views
package html.swiss

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.markdownLinksOrRichText
import lila.swiss.Swiss
import lila.gathering.Condition
import lila.gathering.Condition.WithVerdicts

object side:

  private val separator = " • "

  def apply(
      s: Swiss,
      verdicts: WithVerdicts,
      streamers: List[UserId],
      chat: Boolean
  )(using ctx: Context) =
    frag(
      div(cls := "swiss__meta")(
        st.section(dataIcon := s.perfType.iconChar.toString)(
          div(
            p(
              s.clock.show,
              separator,
              views.html.game.bits.variantLink(s.variant, s.perfType.some, shortName = true),
              separator,
              if (s.settings.rated) trans.ratedTournament() else trans.casualTournament()
            ),
            p(
              span(cls := "swiss__meta__round")(
                trans.swiss.nbRounds.plural(s.settings.nbRounds, s"${s.round}/${s.settings.nbRounds}")
              ),
              separator,
              a(href := routes.Swiss.home)("Swiss"),
              (isGranted(_.ManageTournament) || (ctx.userId.has(s.createdBy) && !s.isFinished)) option frag(
                " ",
                a(href := routes.Swiss.edit(s.id), title := "Edit tournament")(iconTag(""))
              )
            ),
            bits.showInterval(s)
          )
        ),
        s.settings.description map { d =>
          st.section(cls := "description")(markdownLinksOrRichText(d))
        },
        s.looksLikePrize option views.html.tournament.bits.userPrizeDisclaimer(s.createdBy),
        s.settings.position.flatMap(p => lila.tournament.Thematic.byFen(p.opening)) map { pos =>
          div(a(targetBlank, href := pos.url)(pos.name))
        } orElse s.settings.position.map { fen =>
          div(
            "Custom position • ",
            views.html.base.bits.fenAnalysisLink(fen)
          )
        },
        teamLink(s.teamId),
        if (verdicts.relevant)
          st.section(
            dataIcon := (if (ctx.isAuth && verdicts.accepted) "" else ""),
            cls := List(
              "conditions" -> true,
              "accepted"   -> (ctx.isAuth && verdicts.accepted),
              "refused"    -> (ctx.isAuth && !verdicts.accepted)
            )
          )(
            div(
              verdicts.list.sizeIs < 2 option p(trans.conditionOfEntry()),
              verdicts.list map { v =>
                p(
                  cls := List(
                    "condition" -> true,
                    "accepted"  -> (ctx.isAuth && v.verdict.accepted),
                    "refused"   -> (ctx.isAuth && !v.verdict.accepted)
                  ),
                  title := v.verdict.reason.map(_(ctx.lang))
                )(v.verdict match {
                  case Condition.RefusedUntil(until) =>
                    frag(
                      "Because you missed your last swiss game, you cannot enter a new swiss tournament until ",
                      absClientInstant(until),
                      "."
                    )
                  case _ => v.condition.name(s.perfType)
                })
              }
            )
          )
        else br,
        small(trans.by(userIdLink(s.createdBy.some))),
        br,
        absClientInstant(s.startsAt)
      ),
      streamers.nonEmpty option div(cls := "context-streamers")(
        streamers map views.html.streamer.bits.contextual
      ),
      chat option views.html.chat.frag
    )
