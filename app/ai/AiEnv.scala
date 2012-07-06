package lila
package ai

import com.mongodb.casbah.MongoCollection
import scalaz.effects._

import core.Settings

final class AiEnv(settings: Settings) {

  import settings._

  lazy val ai: () ⇒ Ai = (AiChoice, isClient) match {
    case (AiStockfish, true)  ⇒ () ⇒ stockfishClient or stockfishAi
    case (AiStockfish, false) ⇒ () ⇒ stockfishAi
    case (AiCrafty, true)     ⇒ () ⇒ craftyClient or craftyAi
    case (AiCrafty, false)    ⇒ () ⇒ craftyAi
    case _                    ⇒ () ⇒ stupidAi
  }

  lazy val craftyAi = new crafty.Ai(
    server = craftyServer)

  lazy val craftyClient = new crafty.Client(
    playUrl = AiCraftyPlayUrl)

  lazy val craftyServer = new crafty.Server(
    execPath = AiCraftyExecPath,
    bookPath = AiCraftyBookPath)

  lazy val stockfishAi = new stockfish.Ai(
    server = stockfishServer)

  lazy val stockfishClient = new stockfish.Client(
    playUrl = AiStockfishPlayUrl,
    analyseUrl = AiStockfishAnalyseUrl)

  lazy val stockfishServer = new stockfish.Server(
    execPath = AiStockfishExecPath,
    playConfig = stockfishPlayConfig,
    analyseConfig = stockfishAnalyseConfig)

  lazy val stockfishPlayConfig = new stockfish.PlayConfig(settings)
  lazy val stockfishAnalyseConfig = new stockfish.AnalyseConfig(settings)

  lazy val stupidAi = new StupidAi

  lazy val isClient = AiClientMode
  lazy val isServer = AiServerMode

  lazy val clientDiagnose = client.fold(_.diagnose, io())

  def clientPing = client flatMap (_.currentPing)

  def stockfishServerReport = (isServer && AiChoice == AiStockfish) option {
    stockfishServer.report
  }

  private lazy val client = isClient option {
    AiChoice match {
      case AiStockfish ⇒ stockfishClient
      case AiCrafty    ⇒ craftyClient
    }
  }

  private lazy val server = (isServer && AiChoice == AiStockfish) option stockfishServer
}
