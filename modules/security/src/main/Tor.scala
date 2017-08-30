package lila.security

import lila.common.IpAddress

import old.play.Env.WS

final class Tor(providerUrl: String) {

  private var ips = Set.empty[IpAddress]

  private[security] def refresh(withIps: Iterable[IpAddress] => Funit) {
    WS.url(providerUrl).get() map { res =>
      ips = res.body.lines.filterNot(_ startsWith "#").map(IpAddress.apply).toSet
      withIps(ips)
      lila.mon.security.tor.node(ips.size)
    }
  }

  def isExitNode(ip: IpAddress) = ips contains ip
}
