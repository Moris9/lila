package lila.api

import play.api.libs.json._

import chess.format.pgn.Pgn
import lila.analyse.Analysis
import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.Pov
import lila.pref.Pref
import lila.round.JsonView
import lila.security.Granter
import lila.simul.Simul
import lila.tournament.{ Tournament, TournamentRepo }
import lila.user.User

private[api] final class RoundApi(
    jsonView: JsonView,
    noteApi: lila.round.NoteApi,
    analysisApi: AnalysisApi,
    getSimul: Simul.ID => Fu[Option[Simul]],
    lightUser: String => Option[LightUser]) {

  def player(pov: Pov, apiVersion: Int)(implicit ctx: Context): Fu[JsObject] =
    jsonView.playerJson(pov, ctx.pref, apiVersion, ctx.me,
      withBlurs = ctx.me ?? Granter(_.ViewBlurs)) zip
      (pov.game.tournamentId ?? TournamentRepo.byId) zip
      (pov.game.simulId ?? getSimul) zip
      (ctx.me ?? (me => noteApi.get(pov.gameId, me.id))) map {
        case (((json, tourOption), simulOption), note) => (
          blindMode _ compose
          withTournament(pov, tourOption)_ compose
          withSimul(pov, simulOption)_ compose
          withNote(note)_
        )(json)
      }

  def watcher(pov: Pov, apiVersion: Int, tv: Option[Boolean],
    analysis: Option[(Pgn, Analysis)] = None,
    initialFen: Option[Option[String]] = None,
    withMoveTimes: Boolean = false)(implicit ctx: Context): Fu[JsObject] =
    jsonView.watcherJson(pov, ctx.pref, apiVersion, ctx.me, tv,
      withBlurs = ctx.me ?? Granter(_.ViewBlurs),
      initialFen = initialFen,
      withMoveTimes = withMoveTimes) zip
      (pov.game.tournamentId ?? TournamentRepo.byId) zip
      (pov.game.simulId ?? getSimul) zip
      (ctx.me ?? (me => noteApi.get(pov.gameId, me.id))) map {
        case (((json, tourOption), simulOption), note) => (
          blindMode _ compose
          withTournament(pov, tourOption)_ compose
          withSimul(pov, simulOption)_ compose
          withNote(note)_ compose
          withAnalysis(analysis)_
        )(json)
      }

  private def withNote(note: String)(json: JsObject) =
    if (note.isEmpty) json else json + ("note" -> JsString(note))

  private def withAnalysis(a: Option[(Pgn, Analysis)])(json: JsObject) = a.fold(json) {
    case (pgn, analysis) => json + ("analysis" -> Json.obj(
      "moves" -> analysisApi.game(analysis, pgn),
      "white" -> analysisApi.player(chess.Color.White)(analysis),
      "black" -> analysisApi.player(chess.Color.Black)(analysis)
    ))
  }

  private def withTournament(pov: Pov, tourOption: Option[Tournament])(json: JsObject) =
    tourOption.fold(json) { tour =>
      val pairing = tour.pairingOfGameId(pov.gameId)
      json + ("tournament" -> Json.obj(
        "id" -> tour.id,
        "name" -> tour.name,
        "running" -> tour.isRunning,
        "berserkable" -> tour.berserkable,
        "berserk1" -> pairing.map(_.berserk1).filter(0!=),
        "berserk2" -> pairing.map(_.berserk2).filter(0!=)
      ).noNull)
    }

  private def withSimul(pov: Pov, simulOption: Option[Simul])(json: JsObject) =
    simulOption.fold(json) { simul =>
      json + ("simul" -> Json.obj(
        "id" -> simul.id,
        "hostId" -> simul.hostId,
        "name" -> simul.name,
        "nbPlaying" -> simul.playingPairings.size
      ))
    }

  private def blindMode(js: JsObject)(implicit ctx: Context) =
    ctx.blindMode.fold(js + ("blind" -> JsBoolean(true)), js)
}
