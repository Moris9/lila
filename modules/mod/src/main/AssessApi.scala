package lila.mod

import akka.actor.ActorSelection
import lila.analyse.{ Analysis, AnalysisRepo }
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import lila.evaluation.{ AccountAction, Analysed, GameAssessment, PlayerAssessment, PlayerAggregateAssessment, PlayerFlags, PlayerAssessments, Assessible }
import lila.game.{ Game, Player, GameRepo }
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent._

import chess.Color


final class AssessApi(
  collAssessments: Coll,
  logApi: ModlogApi,
  modApi: ModApi,
  reporter: ActorSelection,
  analyser: ActorSelection,
  userIdsSharingIp: String => Fu[List[String]]) {

  import PlayerFlags.playerFlagsBSONHandler

  private implicit val playerAssessmentBSONhandler = Macros.handler[PlayerAssessment]

  def createPlayerAssessment(assessed: PlayerAssessment) =
    collAssessments.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true).void

  def getPlayerAssessmentById(id: String) =
    collAssessments.find(BSONDocument("_id" -> id))
    .one[PlayerAssessment]

  def getPlayerAssessmentsByUserId(userId: String, nb: Int = 100) =
    collAssessments.find(BSONDocument("userId" -> userId))
    .sort(BSONDocument("date" -> -1))
    .cursor[PlayerAssessment]
    .collect[List](nb)

  def getResultsByGameIdAndColor(gameId: String, color: Color) =
    getPlayerAssessmentById(gameId + "/" + color.name)

  def getGameResultsById(gameId: String) =
    getResultsByGameIdAndColor(gameId, Color.White) zip
    getResultsByGameIdAndColor(gameId, Color.Black) map {
      a => PlayerAssessments(a._1, a._2)
    }

  def getPlayerAggregateAssessment(userId: String, nb: Int = 100): Fu[Option[PlayerAggregateAssessment]] = {
    val relatedUsers = userIdsSharingIp(userId)

    UserRepo.byId(userId) zip
    getPlayerAssessmentsByUserId(userId, nb) zip
    relatedUsers zip
    (relatedUsers flatMap UserRepo.filterByEngine) map {
      case (((Some(user), assessedGamesHead :: assessedGamesTail), relatedUs), relatedCheaters) =>
        Some(PlayerAggregateAssessment(
          user,
          assessedGamesHead :: assessedGamesTail,
          relatedUs,
          relatedCheaters))
      case _ => none
    }
  }

  def refreshAssess(gameId: String): Funit =
    GameRepo.game(gameId) zip
      AnalysisRepo.doneById(gameId) flatMap {
        case (Some(g), Some(a)) => onAnalysisReady(g, a)
        case _ => funit
      }

  def refreshAssessByUsername(username: String): Funit = withUser(username) { user =>
    (GameRepo.gamesForAssessment(user.id, 100) flatMap { gs =>
      (gs map {
        g => AnalysisRepo.doneById(g.id) flatMap {
          case Some(a) => onAnalysisReady(g, a, false)
          case _ => funit
        }
      }).sequenceFu.void
    }) >> assessPlayer(user.id)
  }

  def onAnalysisReady(game: Game, analysis: Analysis, assess: Boolean = true): Funit = {
    if (!game.isCorrespondence && game.playedTurns >= 40 && game.mode.rated) {
      val gameAssessments: PlayerAssessments = Assessible(Analysed(game, analysis)).assessments
      gameAssessments.white.fold(funit){createPlayerAssessment} >>
      gameAssessments.black.fold(funit){createPlayerAssessment}
    } else funit
  } >> (if (assess) {
    game.whitePlayer.userId.fold(funit){assessPlayer} >> game.blackPlayer.userId.fold(funit){assessPlayer}
  } else funit)

  def assessPlayer(userId: String): Funit = getPlayerAggregateAssessment(userId) flatMap {
    case Some(playerAggregateAssessment) => playerAggregateAssessment.action match {
      case AccountAction.Engine | AccountAction.EngineAndBan =>
        modApi.autoAdjust(userId)
      case AccountAction.Report =>
        reporter ! lila.hub.actorApi.report.Cheater(userId, playerAggregateAssessment.reportText(3))
        funit
      case AccountAction.Nothing => funit
    }
    case none => funit
  }

  def onGameReady(game: Game): Funit = {
    import lila.evaluation.Statistics.{ skip, coefVariation }

    def manyBlurs(player: Player) =
      player.blurs.toDouble / game.playerMoves(player.color) >= 0.7

    def moveTimes(color: Color): List[Int] =
      skip(game.moveTimes.toList, if (color == Color.White) 0 else 1)

    def consistentMoveTimes(player: Player): Boolean =
      moveTimes(player.color).toNel.map(coefVariation).fold(false)(_ < 0.5)

    def shouldAnalyse =
      if (game.isCorrespondence) false
      else if (game.playedTurns < 40) false
      else if (!game.mode.rated) false
      else if (!game.analysable) false
      // someone is using a bot
      else if (game.players.exists(_.hasSuspiciousHoldAlert)) true
      // don't analyse bullet games
      else if (game.speed == chess.Speed.Bullet) false
      // someone blurs a lot
      else if (game.players exists manyBlurs) true
      // someone has consistent move times
      else if (game.players exists consistentMoveTimes) true
      else false

    if (shouldAnalyse) analyser ! lila.hub.actorApi.ai.AutoAnalyse(game.id)

    funit
  }

  private def withUser[A](username: String)(op: User => Fu[A]): Fu[A] =
    UserRepo named username flatten "[mod] missing user " + username flatMap op

}
