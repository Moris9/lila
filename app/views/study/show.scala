package views.html.study

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
    s: lila.study.Study,
    data: lila.study.JsonView.JsData,
    chatOption: Option[lila.chat.UserChat.Mine],
    socketVersion: lila.socket.Socket.SocketVersion,
    streams: List[lila.streamer.Stream]
  )(implicit ctx: Context) = views.html.base.layout(
    title = s.name.value,
    moreCss = cssTag("analyse.study"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJs(s"""lichess=window.lichess||{};lichess.study={
study: ${safeJsonValue(data.study)},
data: ${safeJsonValue(data.analysis)},
i18n: ${views.html.board.userAnalysisI18n()},
tagTypes: '${lila.study.PgnTags.typesToString}',
userId: $jsUserIdString,
chat: ${
        chatOption.fold("null")(c => safeJsonValue(views.html.chat.json(
          c.chat,
          name = trans.chatRoom.txt(),
          timeout = c.timeout,
          writeable = ctx.userId.??(s.canChat),
          public = false,
          localMod = ctx.userId.??(s.canContribute)
        )))
      },
explorer: {
endpoint: "$explorerEndpoint",
tablebaseEndpoint: "$tablebaseEndpoint"
},
socketUrl: "${routes.Study.websocket(s.id.value, apiVersion.value)}",
socketVersion: $socketVersion
};""")
    ),
    robots = s.isPublic,
    chessground = false,
    zoomable = true,
    openGraph = lila.app.ui.OpenGraph(
      title = s.name.value,
      url = s"$netBaseUrl${routes.Study.show(s.id.value).url}",
      description = s"A chess study by ${usernameOrId(s.ownerId)}"
    ).some
  )(frag(
      main(cls := "analyse"),
      bits.streamers(streams)
    ))
}
