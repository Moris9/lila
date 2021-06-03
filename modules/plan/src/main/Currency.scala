package lila.plan

import io.methvin.play.autoconfig.AutoConfig
import java.util.{ Currency, Locale }
import play.api.i18n.Lang
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

  import CurrencyApi._

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

  def convert(money: Money, currency: Currency): Fu[Option[Money]] =
    ratesCache.get {} map { rates =>
      for {
        fromRate <- rates get money.currencyCode
        toRate   <- rates get currency.getCurrencyCode
      } yield Money(money.amount / fromRate * toRate, currency)
    }

  def toUsd(money: Money): Fu[Option[Usd]] =
    ratesCache.get {} map { rates =>
      rates.get(money.currencyCode) map { fromRate =>
        Usd(money.amount / fromRate)
      }
    }

  val USD = Currency getInstance "USD"

  def currencyByCountryCodeOrLang(countryCode: Option[String], lang: Lang): Currency =
    countryCode
      .flatMap { code => scala.util.Try(new java.util.Locale("", code)).toOption }
      .flatMap(currencyOption)
      .filter(stripeCurrencies.contains)
      .orElse(currencyOption(lang.locale))
      .filter(stripeCurrencies.contains)
      .getOrElse(USD)
}

private object CurrencyApi {

  case class Config(appId: config.Secret)
  implicit val currencyConfigLoader = AutoConfig.loader[Config]

  private def currencyOption(locale: Locale) = Try(Currency getInstance locale).toOption
  private def currencyOption(code: String)   = Try(Currency getInstance code).toOption

  val zeroDecimalCurrencies: Set[Currency] = Set(
    "BIF",
    "CLP",
    "DJF",
    "GNF",
    "JPY",
    "KMF",
    "KRW",
    "MGA",
    "PYG",
    "RWF",
    "UGX",
    "VND",
    "VUV",
    "XAF",
    "XOF",
    "XPF"
  ).flatMap(currencyOption)

  val stripeCurrencies: Set[Currency] = Set(
    "USD",
    "AED",
    "AFN",
    "ALL",
    "AMD",
    "ANG",
    "AOA",
    "ARS",
    "AUD",
    "AWG",
    "AZN",
    "BAM",
    "BBD",
    "BDT",
    "BGN",
    "BIF",
    "BMD",
    "BND",
    "BOB",
    "BRL",
    "BSD",
    "BWP",
    "BZD",
    "CAD",
    "CDF",
    "CHF",
    "CLP",
    "CNY",
    "COP",
    "CRC",
    "CVE",
    "CZK",
    "DJF",
    "DKK",
    "DOP",
    "DZD",
    "EGP",
    "ETB",
    "EUR",
    "FJD",
    "FKP",
    "GBP",
    "GEL",
    "GIP",
    "GMD",
    "GNF",
    "GTQ",
    "GYD",
    "HKD",
    "HNL",
    "HRK",
    "HTG",
    "HUF",
    "IDR",
    "ILS",
    "INR",
    "ISK",
    "JMD",
    "JPY",
    "KES",
    "KGS",
    "KHR",
    "KMF",
    "KRW",
    "KYD",
    "KZT",
    "LAK",
    "LBP",
    "LKR",
    "LRD",
    "LSL",
    "MAD",
    "MDL",
    "MGA",
    "MKD",
    "MMK",
    "MNT",
    "MOP",
    "MRO",
    "MUR",
    "MVR",
    "MWK",
    "MXN",
    "MYR",
    "MZN",
    "NAD",
    "NGN",
    "NIO",
    "NOK",
    "NPR",
    "NZD",
    "PAB",
    "PEN",
    "PGK",
    "PHP",
    "PKR",
    "PLN",
    "PYG",
    "QAR",
    "RON",
    "RSD",
    "RUB",
    "RWF",
    "SAR",
    "SBD",
    "SCR",
    "SEK",
    "SGD",
    "SHP",
    "SLL",
    "SOS",
    "SRD",
    "STD",
    "SZL",
    "THB",
    "TJS",
    "TOP",
    "TRY",
    "TTD",
    "TWD",
    "TZS",
    "UAH",
    "UGX",
    "UYU",
    "UZS",
    "VND",
    "VUV",
    "WST",
    "XAF",
    "XCD",
    "XOF",
    "XPF",
    "YER",
    "ZAR",
    "ZMW"
  ).flatMap(currencyOption)
}
