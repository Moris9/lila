package lila.plan

import io.methvin.play.autoconfig.AutoConfig
import java.util.{ Currency, Locale }
import play.api.libs.json._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.Try

import lila.common.config

case class CurrencyWithRate(currency: Currency, rate: Double)

final class CurrencyApi(
    ws: StandaloneWSClient,
    mongoCache: lila.memo.MongoCache.Api,
    config: CurrencyApi.Config
)(implicit ec: ExecutionContext) {

  private val baseUrl = "https://openexchangerates.org/api"

  private val ratesCache = mongoCache.unit[Map[String, Double]](
    "currency:rates",
    60 minutes // i.e. 744/month, under the 1000/month limit of free OER plan
  ) { loader =>
    _.refreshAfterWrite(61 minutes)
      .buildAsyncFuture {
        loader { _ =>
          ws.url(s"$baseUrl/latest.json")
            .withQueryStringParameters("app_id" -> config.appId.value)
            .get()
            .dmap { res =>
              (res.body[JsValue] \ "rates").validate[Map[String, Double]].asOpt.fold(Map("USD" -> 1d)) {
                _.filterValues(0 <)
              }
            }
        }
      }
  }

  def convert(money: Money, to: Locale): Fu[Option[Money]] =
    ratesCache.get {} map { rates =>
      for {
        currency <- Try(Currency getInstance to).toOption
        fromRate <- rates get money.currency.getCurrencyCode
        toRate   <- rates get currency.getCurrencyCode
      } yield Money(money.amount * fromRate / toRate, to)
    }

  def convertFromUsd(amounts: List[BigDecimal], to: Locale): Fu[Option[List[Money]]] =
    ratesCache.get {} map { rates =>
      for {
        currency <- Try(Currency getInstance to).toOption
        rate     <- rates get currency.getCurrencyCode
      } yield amounts map { a => Money(a / rate, to) }
    }

  val US  = Locale.US
  val USD = Currency getInstance US

  def hasCurrency(locale: Locale) = Try(Currency getInstance locale).isSuccess

  def byCountryCode(countryCode: Option[String]): Fu[CurrencyWithRate] =
    countryCode
      .flatMap { country =>
        Try(new Locale("", country)).toOption
      }
      .?? { locale =>
        Try(Currency getInstance locale).toOption ?? { currency =>
          ratesCache.get(()) map {
            _ get currency.getCurrencyCode map {
              CurrencyWithRate(currency, _)
            }
          }
        }
      } dmap {
      _ | CurrencyWithRate(USD, 1d)
    }
}

private object CurrencyApi {

  case class Config(appId: config.Secret)
  implicit val currencyConfigLoader = AutoConfig.loader[Config]
}
