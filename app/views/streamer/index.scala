package views.html.streamer

import controllers.routes

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.i18n.LangList

object index:

  import trans.streamer.*

  private val dataDedup = attr("data-dedup")

  def apply(
      live: List[lila.streamer.Streamer.WithUserAndStream],
      pager: Paginator[lila.streamer.Streamer.Context],
      requests: Boolean
  )(implicit ctx: Context) =

    val title = if (requests) "Streamer approval requests" else lichessStreamers.txt()

    def widget(s: lila.streamer.Streamer.Context, stream: Option[lila.streamer.Stream]) =
      frag(
        if (requests) a(href := s"${routes.Streamer.edit}?u=${s.user.username}", cls := "overlay")
        else bits.redirectLink(s.user.username, stream.isDefined.some)(cls := "overlay"),
        stream.isDefined option span(cls := "live-ribbon")(span(trans.streamer.live())),
        div(cls := "picture")(
          picture.thumbnail(s.streamer, s.user),
          !requests && ctx.me.nonEmpty option span(cls := "subscribe-ribbon-bottom-left")(
            span(
              input(
                cls        := "std-toggle subscribe-switch",
                tpe        := "checkbox",
                formaction := s"${routes.Streamer.subscribe(s.streamer.userId, !s.subscribed)}",
                s.subscribed option st.checked
              ),
              trans.subscribe()
            )
          )
        ),
        div(cls := "overview")(
          bits.streamerTitle(s),
          s.streamer.headline.map(_.value).map { d =>
            p(
              cls := s"headline ${if (d.length < 60) "small" else if (d.length < 120) "medium" else "large"}"
            )(d)
          },
          div(cls := "services")(
            s.streamer.twitch.map { twitch =>
              div(cls := "service twitch")(twitch.minUrl)
            },
            s.streamer.youTube.map { youTube =>
              div(cls := "service youTube")(youTube.minUrl)
            }
          ),
          div(cls := "ats")(
            stream.map { s =>
              p(cls := "at")(
                currentlyStreaming(strong(s.cleanStatus))
              )
            } getOrElse frag(
              p(cls := "at")(trans.lastSeenActive(momentFromNow(s.streamer.seenAt))),
              s.streamer.liveAt.map { liveAt =>
                p(cls := "at")(lastStream(momentFromNow(liveAt)))
              }
            )
          )
        )
      )

    views.html.base.layout(
      title = title,
      moreCss = cssTag("streamer.list"),
      moreJs = infiniteScrollTag
    ) {
      main(cls                                                          := "page-menu")(
        bits.menu(if (requests) "requests" else "index", none)(ctx)(cls := " page-menu__menu"),
        div(cls := "page-menu__content box streamer-list")(
          boxTop(h1(dataIcon := "", cls := "text")(title)),
          !requests option div(cls := "list live")(
            live.map { s =>
              st.article(cls := "streamer")(widget(s, s.stream))
            }
          ),
          div(cls := "list infinite-scroll")(
            (live.size % 2 == 1) option div(cls := "none"),
            pager.currentPageResults.map { s =>
              st.article(cls := "streamer paginated", dataDedup := s.streamer.id.value)(widget(s, none))
            },
            pagerNext(
              pager,
              np =>
                addQueryParameter(
                  addQueryParameter(routes.Streamer.index().url, "page", np),
                  "requests",
                  if (requests) 1 else 0
                )
            )
          )
        )
      )
    }
