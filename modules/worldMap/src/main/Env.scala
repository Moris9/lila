package lila.worldMap

import com.typesafe.config.Config

import com.sanoma.cda.geoip.MaxMindIpGeo
import lila.common.PimpedConfig._

final class Env(
    system: akka.actor.ActorSystem,
    config: Config) {

  private val GeoIPFile = config getString "geoip.file"
  private val GeoIPCacheTtl = config duration "geoip.cache_ttl"
  private val PlayersCacheSize = config getInt "players.cache_size"

  lazy val players = new Players(PlayersCacheSize)

  lazy val stream = new Stream(
    system = system,
    players = players,
    geoIp = MaxMindIpGeo(GeoIPFile, 0),
    geoIpCacheTtl = GeoIPCacheTtl)
}

object Env {

  lazy val current: Env = "worldMap" boot new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "worldMap")
}

