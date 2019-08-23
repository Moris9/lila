package lila.round

import scala.concurrent.duration._
import scala.math.{ log10, sqrt }

import lila.game.Game
import lila.hub.actorApi.Deploy
import lila.hub.actorApi.map.{ Tell, TellIfExists, Exists }
import lila.user.User

private object SocketMap {

  def make(
    makeHistory: Game.ID => History,
    dependencies: RoundSocket.Dependencies,
    socketTimeout: FiniteDuration,
    playban: lila.playban.PlaybanApi
  ): SocketMap = {

    import dependencies._

    val defaultGoneWeight = fuccess(1f)
    def goneWeight(userId: User.ID): Fu[Float] =
      playban.sitAndDcCounter(userId) map { sc =>
        if (sc > -5) 1f
        else (1 - 0.7 * sqrt(log10(-sc - 3))).toFloat atLeast 0.25f
      }
    def goneWeights(game: Game): Fu[(Float, Float)] =
      game.whitePlayer.userId.fold(defaultGoneWeight)(goneWeight) zip
        game.blackPlayer.userId.fold(defaultGoneWeight)(goneWeight)

    lazy val socketMap: SocketMap = lila.socket.SocketMap[RoundSocket](
      system = system,
      mkTrouper = (id: Game.ID) => new RoundSocket(
        dependencies = dependencies,
        gameId = id,
        history = makeHistory(id),
        keepMeAlive = () => socketMap touch id,
        getGoneWeights = goneWeights
      ),
      accessTimeout = socketTimeout,
      monitoringName = "round.socketMap",
      broomFrequency = 4001 millis
    )
    system.lilaBus.subscribeFuns(
      'startGame -> {
        case msg: lila.game.actorApi.StartGame => socketMap.tellIfPresent(msg.game.id, msg)
      },
      'roundSocket -> {
        case TellIfExists(id, msg) => socketMap.tellIfPresent(id, msg)
        case Tell(id, msg) => socketMap.tell(id, msg)
        case Exists(id, promise) => promise success socketMap.exists(id)
      },
      'deploy -> {
        case m: Deploy => socketMap tellAll m
      }
    )
    socketMap
  }
}
