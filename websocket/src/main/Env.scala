package lila.websocket

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(config: Config) {

}

object Env {

  lazy val current = "[websocket] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "websocket")
}
