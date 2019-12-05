package lila.search

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws._

@Module
private class SearchConfig(
    val enabled: Boolean,
    val writeable: Boolean,
    val endpoint: String
)

@Module
final class Env(
    appConfig: Configuration,
    ws: WSClient
)(implicit system: ActorSystem) {

  private val config = appConfig.get[SearchConfig]("search")(AutoConfig.loader)

  private def makeHttp(index: Index): ESClientHttp = wire[ESClientHttp]

  val makeClient = (index: Index) =>
    if (config.enabled) makeHttp(index)
    else wire[ESClientStub]
}
