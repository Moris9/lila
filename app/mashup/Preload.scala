package lila.app
package mashup

import lila.lobby.{ Hook, HookRepo }
import lila.lobby.actorApi.lobby._
import lila.timeline.GameEntry
import lila.game.{ Game, GameRepo, Featured }
import lila.forum.PostLiteView
import lila.socket.History
import lila.tournament.Created
import lila.setup.FilterConfig
import lila.user.{ User, Context }
import controllers.routes
import makeTimeout.large

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.mvc.Call
import play.api.libs.json.{ Json, JsObject, JsArray }

final class Preload(
    lobby: ActorRef,
    history: History,
    featured: Featured) {

  private type RightResponse = (JsObject, List[GameEntry], List[PostLiteView], List[Created], Option[Game])
  private type Response = Either[Call, RightResponse]

  def apply(
    timeline: Fu[List[GameEntry]],
    posts: Fu[List[PostLiteView]],
    tours: Fu[List[Created]],
    filter: Fu[FilterConfig])(implicit ctx: Context): Fu[Response] =
    ctx.isAuth.fold(lobby ? GetOpen, lobby ? GetOpenCasual).mapTo[List[Hook]] zip
      timeline zip 
      posts zip 
      tours zip 
      featured.one zip 
      filter map { 
        case (((((hooks, entries), posts), tours), feat), filter) ⇒
          (Right((Json.obj(
            "version" -> history.version,
            "pool" -> JsArray(hooks map (_.render)),
            "filter" -> filter.render
          ), entries, posts, tours, feat)))
      }
}
