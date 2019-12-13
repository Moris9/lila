package lila.common

import scala.concurrent.duration.FiniteDuration
import io.methvin.play.autoconfig._
import scala.jdk.CollectionConverters._
import play.api.ConfigLoader

object config {

  case class CollName(value: String) extends AnyVal with StringValue

  case class Secret(value: String) extends AnyVal {
    override def toString = "Secret(****)"
  }

  case class BaseUrl(value: String) extends AnyVal with StringValue

  case class AppPath(value: java.io.File) extends AnyVal

  case class Max(value: Int) extends AnyVal with IntValue with Ordered[Int] {
    def compare(other: Int) = Integer.compare(value, other)
  }
  case class MaxPerPage(value: Int) extends AnyVal with IntValue

  case class MaxPerSecond(value: Int) extends AnyVal with IntValue

  case class NetDomain(value: String)   extends AnyVal with StringValue
  case class AssetDomain(value: String) extends AnyVal with StringValue

  case class NetConfig(
      domain: NetDomain,
      protocol: String,
      @ConfigName("base_url") baseUrl: BaseUrl,
      @ConfigName("asset.domain") assetDomain: AssetDomain,
      @ConfigName("socket.domain") socketDomain: String,
      crawlable: Boolean,
      @ConfigName("ratelimit") rateLimit: Boolean,
      email: EmailAddress,
      ip: IpAddress
  )

  implicit val maxLoader          = intLoader(Max.apply)
  implicit val maxPerPageLoader   = intLoader(MaxPerPage.apply)
  implicit val maxPerSecondLoader = intLoader(MaxPerSecond.apply)
  implicit val collNameLoader     = strLoader(CollName.apply)
  implicit val secretLoader       = strLoader(Secret.apply)
  implicit val baseUrlLoader      = strLoader(BaseUrl.apply)
  implicit val emailAddressLoader = strLoader(EmailAddress.apply)
  implicit val netDomainLoader    = strLoader(NetDomain.apply)
  implicit val assetDomainLoader  = strLoader(AssetDomain.apply)
  implicit val ipLoader           = strLoader(IpAddress.apply)
  implicit val netLoader          = AutoConfig.loader[NetConfig]

  implicit val strListLoader: ConfigLoader[List[String]] = ConfigLoader { c => k =>
    c.getStringList(k).asScala.toList
  }
  implicit def listLoader[A](implicit l: ConfigLoader[A]): ConfigLoader[List[A]] = ConfigLoader { c => k =>
    c.getConfigList(k).asScala.toList map { l.load(_) }
  }

  def strLoader[A](f: String => A): ConfigLoader[A]              = ConfigLoader(_.getString) map f
  def intLoader[A](f: Int => A): ConfigLoader[A]                 = ConfigLoader(_.getInt) map f
  def boolLoader[A](f: Boolean => A): ConfigLoader[A]            = ConfigLoader(_.getBoolean) map f
  def durationLoader[A](f: FiniteDuration => A): ConfigLoader[A] = ConfigLoader(_.duration) map f
}
