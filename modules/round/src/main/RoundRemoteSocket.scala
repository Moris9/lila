package lila.round

import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import actorApi._
import actorApi.round._
import chess.format.Uci
import chess.{ Color, White, Black, Speed, Centis, MoveMetrics }
import lila.chat.Chat
import lila.common.{ Bus, IpAddress }
import lila.game.Game.{ PlayerId, FullId }
import lila.game.{ Game, Event }
import lila.hub.actorApi.round.{ Berserk, RematchYes, RematchNo, Abort, Resign }
import lila.hub.{ Trouper, TrouperMap, DuctMap }
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, SocketVersion, GetVersion, makeMessage }
import lila.user.User

final class RoundRemoteSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    roundDependencies: RoundRemoteDuct.Dependencies,
    deployPersistence: DeployPersistence,
    scheduleExpiration: Game => Unit,
    chatActor: akka.actor.ActorSelection,
    tournamentActor: akka.actor.ActorSelection,
    selfReport: SelfReport,
    messenger: Messenger,
    goneWeightsFor: Game => Fu[(Float, Float)],
    system: akka.actor.ActorSystem
) {

  import RoundRemoteSocket._

  def getGame(gameId: Game.ID): Fu[Option[Game]] = rounds.getOrMake(gameId).getGame addEffect { g =>
    if (!g.isDefined) rounds kill gameId
  }
  def gameIfPresent(gameId: Game.ID): Fu[Option[Game]] = rounds.getIfPresent(gameId).??(_.getGame)
  def updateIfPresent(game: Game): Fu[Game] = rounds.getIfPresent(game.id).fold(fuccess(game))(_.getGame.map(_ | game))

  val rounds = new DuctMap[RoundRemoteDuct](
    mkDuct = id => {
      val duct = new RoundRemoteDuct(
        dependencies = roundDependencies,
        gameId = id,
        isGone = id => ???,
        socketSend = send
      )(new GameProxy(id, deployPersistence.isEnabled, system.scheduler))
      duct.getGame foreach {
        _ foreach { game =>
          scheduleExpiration(game)
          goneWeightsFor(game) map { RoundRemoteDuct.SetGameInfo(game, _) } foreach duct.!
        }
      }
      duct
    },
    accessTimeout = 40 seconds
  )

  def tellRound(gameId: Game.Id, msg: Any): Unit = rounds.tell(gameId.value, msg)

  private lazy val roundHandler: Handler = {
    case ping: Protocol.In.PlayerPing => tellRound(ping.gameId, ping)
    case Protocol.In.PlayerDo(id, tpe, o) => tpe match {
      case "moretime" => tellRound(id.gameId, Moretime(id.playerId))
      case "rematch-yes" => tellRound(id.gameId, RematchYes(id.playerId.value))
      case "rematch-no" => tellRound(id.gameId, RematchNo(id.playerId.value))
      case "takeback-yes" => tellRound(id.gameId, TakebackYes(id.playerId))
      case "takeback-no" => tellRound(id.gameId, TakebackNo(id.playerId))
      case "draw-yes" => tellRound(id.gameId, DrawYes(id.playerId))
      case "draw-no" => tellRound(id.gameId, DrawNo(id.playerId))
      case "draw-claim" => tellRound(id.gameId, DrawClaim(id.playerId))
      case "resign" => tellRound(id.gameId, Resign(id.playerId.value))
      case "resign-force" => tellRound(id.gameId, ResignForce(id.playerId))
      case "draw-force" => tellRound(id.gameId, DrawForce(id.playerId))
      case "abort" => tellRound(id.gameId, Abort(id.playerId.value))
      case "outoftime" => tellRound(id.gameId, QuietFlag) // mobile app BC
      case "bye2" => tellRound(id.gameId, ByePlayer(id.playerId))
      case t => logger.warn(s"Unhandled round socket message: $t")
    }
    case Protocol.In.AnyDo(gameId, playerId, tpe, o) => tpe match {
      case "flag" => o str "d" flatMap Color.apply map { c =>
        tellRound(gameId, ClientFlag(c, playerId))
      }
    }
    case c: Protocol.In.PlayerChatSay => tellRound(c.gameId, c)
    case Protocol.In.WatcherChatSay(gameId, userId, msg) => messenger.watcher(gameId.value, userId, msg)
    case Protocol.In.PlayerMove(fullId, uci, blur, lag) =>
      // TODO remove promise, resync from remote round duct
      val promise = Promise[Unit]
      promise.future onFailure { case _: Exception => send(Protocol.Out.resyncPlayer(fullId)) }
      tellRound(fullId.gameId, HumanPlay(fullId.playerId, uci, blur, lag, promise.some))
    case Protocol.In.Berserk(gameId, userId) => tournamentActor ! Berserk(gameId.value, userId)
    case RP.In.KeepAlives(roomIds) => roomIds foreach { roomId =>
      rounds touchOrMake roomId.value
    }
    case RP.In.TellRoomSri(gameId, P.In.TellSri(sri, user, tpe, o)) => tpe match {
      case t => logger.warn(s"Unhandled round socket message: $t")
    }
    case hold: Protocol.In.HoldAlert => tellRound(hold.fullId.gameId, hold)
    case Protocol.In.SelfReport(fullId, ip, userId, name) => selfReport(userId, ip, fullId, name)
  }

  private lazy val send: String => Unit = remoteSocketApi.makeSender("round-out").apply _

  remoteSocketApi.subscribe("round-in", Protocol.In.reader)(
    roundHandler orElse remoteSocketApi.baseHandler
  )
}

object RoundRemoteSocket {

  object Protocol {

    object In {

      case class PlayerPing(gameId: Game.Id, color: Color) extends P.In
      case class PlayerDo(fullId: FullId, tpe: String, msg: JsObject) extends P.In
      case class PlayerMove(fullId: FullId, uci: Uci, blur: Boolean, lag: MoveMetrics) extends P.In
      case class PlayerChatSay(gameId: Game.Id, userIdOrColor: Either[User.ID, Color], msg: String) extends P.In
      case class WatcherChatSay(gameId: Game.Id, userId: User.ID, msg: String) extends P.In
      case class AnyDo(gameId: Game.Id, playerId: Option[PlayerId], tpe: String, msg: JsObject) extends P.In
      case class HoldAlert(fullId: FullId, ip: IpAddress, mean: Int, sd: Int) extends P.In
      case class Berserk(gameId: Game.Id, userId: User.ID) extends P.In
      case class SelfReport(fullId: FullId, ip: IpAddress, userId: Option[User.ID], name: String) extends P.In

      val reader: P.In.Reader = raw => raw.path match {
        case "round/w" => PlayerPing(Game.Id(raw.args), chess.White).some
        case "round/b" => PlayerPing(Game.Id(raw.args), chess.Black).some
        case "round/do" => raw.get(2) {
          case Array(fullId, payload) => for {
            obj <- Json.parse(payload).asOpt[JsObject]
            tpe <- obj str "t"
          } yield PlayerDo(FullId(fullId), tpe, obj)
        }
        case "round/do/any" => raw.get(3) {
          case Array(gameId, playerId, payload) => for {
            obj <- Json.parse(payload).asOpt[JsObject]
            tpe <- obj str "t"
          } yield AnyDo(Game.Id(gameId), P.In.optional(playerId) map PlayerId.apply, tpe, obj)
        }
        case "round/move" => raw.get(5) {
          case Array(fullId, uciS, blurS, lagS, mtS) => Uci(uciS) map { uci =>
            PlayerMove(FullId(fullId), uci, P.In.boolean(blurS), MoveMetrics(centis(lagS), centis(mtS)))
          }
        }
        case "chat/say" => raw.get(3) {
          case Array(roomId, author, msg) =>
            val a = author match {
              case "w" => Right(White)
              case "b" => Right(Black)
              case u => Left(u)
            }
            PlayerChatSay(Game.Id(roomId), a, msg).some
        }
        case "chat/say/w" => raw.get(3) {
          case Array(roomId, userId, msg) => WatcherChatSay(Game.Id(roomId), userId, msg).some
        }
        case "round/berserk" => raw.get(2) {
          case Array(gameId, userId) => Berserk(Game.Id(gameId), userId).some
        }
        case "round/hold" => raw.get(4) {
          case Array(fullId, ip, meanS, sdS) => for {
            mean <- parseIntOption(meanS)
            sd <- parseIntOption(sdS)
          } yield HoldAlert(FullId(fullId), IpAddress(ip), mean, sd)
        }
        case "round/report" => raw.get(4) {
          case Array(fullId, ip, user, name) => SelfReport(FullId(fullId), IpAddress(ip), P.In.optional(user), name).some
        }
        case _ => RP.In.reader(raw)
      }

      private def centis(s: String): Option[Centis] =
        if (s == "-") none
        else parseIntOption(s) map Centis.apply
    }

    object Out {

      def resyncPlayer(fullId: FullId) = s"round/resync/player $fullId"
    }
  }
}
