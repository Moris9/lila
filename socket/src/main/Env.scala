package lila.socket

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(config: Config) {

}

object Env {

  lazy val current = "[boot] socket" describes new Env(
    config = lila.common.PlayApp loadConfig "socket")
}
