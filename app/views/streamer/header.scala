package views.html.streamer

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object header {

  import trans.streamer._

  def apply(s: lila.streamer.Streamer.WithUserAndStream, following: Option[Boolean])(implicit ctx: Context) =
    div(cls := "streamer-header")(
      bits.pic(s.streamer, s.user),
      div(cls := "overview")(
        h1(dataIcon := "")(
          titleTag(s.user.title),
          s.streamer.name
        ),
        s.streamer.headline.map(_.value).map { d =>
          p(cls := s"headline ${if (d.length < 60) "small" else if (d.length < 120) "medium" else "large"}")(
            d
          )
        },
        div(cls := "services")(
          s.streamer.twitch.map { twitch =>
            a(
              cls := List(
                "service twitch" -> true,
                "live"           -> s.stream.exists(_.twitch)
              ),
              href := twitch.fullUrl
            )(twitch.minUrl)
          },
          s.streamer.youTube.map { youTube =>
            a(
              cls := List(
                "service youTube" -> true,
                "live"            -> s.stream.exists(_.twitch)
              ),
              href := youTube.fullUrl
            )(youTube.minUrl)
          },
          a(cls := "service lichess", href := routes.User.show(s.user.username))(
            netConfig.domain,
            routes.User.show(s.user.username).url
          )
        ),
        div(cls := "ats")(
          s.stream.map { s =>
            p(cls := "at")(currentlyStreaming(strong(s.status)))
          } getOrElse frag(
            p(cls := "at")(trans.lastSeenActive(momentFromNow(s.streamer.seenAt))),
            s.streamer.liveAt.map { liveAt =>
              p(cls := "at")(lastStream(momentFromNow(liveAt)))
            }
          )
        ),
        following.map { f =>
          (ctx.isAuth && !ctx.is(s.user)) option
            submitButton(
              attr("data-user") := s.user.id,
              dataIcon := "h",
              cls := List(
                "follow button text" -> true,
                "active"             -> f
              )
            )(
              span(cls := "active-no")(trans.follow()),
              span(cls := "active-yes")(trans.following())
            )
        }
      )
    )
}
