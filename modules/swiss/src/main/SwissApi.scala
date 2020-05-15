package lila.swiss

import akka.stream.scaladsl._
import org.joda.time.DateTime
import ornicar.scalalib.Zero
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.duration._
import scala.util.chaining._

import lila.chat.Chat
import lila.common.{ Bus, GreatPlayer, LightUser }
import lila.db.dsl._
import lila.game.Game
import lila.hub.LightTeam.TeamID
import lila.round.actorApi.round.QuietFlag
import lila.user.{ User, UserRepo }

final class SwissApi(
    colls: SwissColls,
    cache: SwissCache,
    userRepo: UserRepo,
    socket: SwissSocket,
    director: SwissDirector,
    scoring: SwissScoring,
    rankingApi: SwissRankingApi,
    standingApi: SwissStandingApi,
    boardApi: SwissBoardApi,
    chatApi: lila.chat.ChatApi,
    lightUserApi: lila.user.LightUserApi,
    roundSocket: lila.round.RoundSocket
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

  private val sequencer =
    new lila.hub.DuctSequencers(
      maxSize = 256,
      expiration = 1 minute,
      timeout = 10 seconds,
      name = "swiss.api"
    )

  import BsonHandlers._

  def byId(id: Swiss.Id)            = colls.swiss.byId[Swiss](id.value)
  def notFinishedById(id: Swiss.Id) = byId(id).dmap(_.filter(_.isNotFinished))
  def createdById(id: Swiss.Id)     = byId(id).dmap(_.filter(_.isCreated))
  def startedById(id: Swiss.Id)     = byId(id).dmap(_.filter(_.isStarted))

  def featurable: Fu[List[Swiss]] = cache.feature.get

  def create(data: SwissForm.SwissData, me: User, teamId: TeamID): Fu[Swiss] = {
    val swiss = Swiss(
      _id = Swiss.makeId,
      name = data.name | GreatPlayer.randomName,
      clock = data.clock,
      variant = data.realVariant,
      round = SwissRound.Number(0),
      nbPlayers = 0,
      nbOngoing = 0,
      createdAt = DateTime.now,
      createdBy = me.id,
      teamId = teamId,
      nextRoundAt = data.realStartsAt.some,
      startsAt = data.realStartsAt,
      finishedAt = none,
      winnerId = none,
      settings = Swiss.Settings(
        nbRounds = data.nbRounds,
        rated = data.rated | true,
        description = data.description,
        hasChat = data.hasChat | true,
        roundInterval = data.realRoundInterval
      )
    )
    colls.swiss.insert.one(addFeaturable(swiss)) >>-
      cache.featuredInTeam.invalidate(swiss.teamId) inject swiss
  }

  def update(swiss: Swiss, data: SwissForm.SwissData): Funit =
    Sequencing(swiss.id)(byId) { old =>
      colls.swiss.update
        .one(
          $id(old.id),
          old.copy(
            name = data.name | old.name,
            clock = data.clock,
            variant = data.realVariant,
            startsAt = data.startsAt.ifTrue(old.isCreated) | old.startsAt,
            nextRoundAt =
              if (old.isCreated) Some(data.startsAt | old.startsAt)
              else old.nextRoundAt,
            settings = old.settings.copy(
              nbRounds = data.nbRounds,
              rated = data.rated | old.settings.rated,
              description = data.description,
              hasChat = data.hasChat | old.settings.hasChat,
              roundInterval = data.roundInterval.fold(old.settings.roundInterval)(_.seconds)
            )
          ) pipe { s =>
            if (
              s.isStarted && s.nbOngoing == 0 && (s.nextRoundAt.isEmpty || old.settings.manualRounds) && !s.settings.manualRounds
            )
              s.copy(nextRoundAt = DateTime.now.plusSeconds(s.settings.roundInterval.toSeconds.toInt).some)
            else if (s.settings.manualRounds && !old.settings.manualRounds)
              s.copy(nextRoundAt = none)
            else s
          }
        )
        .void >>- socket.reload(swiss.id)
    }

  def scheduleNextRound(swiss: Swiss, date: DateTime): Funit =
    Sequencing(swiss.id)(notFinishedById) { old =>
      old.settings.manualRounds ?? {
        if (old.isCreated) colls.swiss.updateField($id(old.id), "startsAt", date).void
        else if (old.isStarted && old.nbOngoing == 0)
          colls.swiss.updateField($id(old.id), "nextRoundAt", date).void >>- {
            val show = org.joda.time.format.DateTimeFormat.forStyle("MS") print date
            systemChat(swiss.id, s"Round ${swiss.round.value + 1} scheduled at $show UTC")
          }
        else funit
      } >>- socket.reload(swiss.id)
    }

  def join(id: Swiss.Id, me: User, isInTeam: TeamID => Boolean): Fu[Boolean] =
    Sequencing(id)(notFinishedById) { swiss =>
      colls.player // try a rejoin first
        .updateField($id(SwissPlayer.makeId(swiss.id, me.id)), SwissPlayer.Fields.absent, false)
        .flatMap { rejoin =>
          fuccess(rejoin.n == 1) >>| { // if the match failed (not the update!), try a join
            (swiss.isEnterable && isInTeam(swiss.teamId)) ?? {
              colls.player.insert.one(SwissPlayer.make(swiss.id, me, swiss.perfLens)) zip
                colls.swiss.update.one($id(swiss.id), $inc("nbPlayers" -> 1)) inject true
            }
          }
        }
    } flatMap { res =>
      recomputeAndUpdateAll(id) inject res
    }

  def withdraw(id: Swiss.Id, me: User): Funit =
    Sequencing(id)(notFinishedById) { swiss =>
      SwissPlayer.fields { f =>
        if (swiss.isStarted)
          colls.player.updateField($id(SwissPlayer.makeId(swiss.id, me.id)), f.absent, true)
        else
          colls.player.delete.one($id(SwissPlayer.makeId(swiss.id, me.id))) flatMap { res =>
            (res.n == 1) ?? colls.swiss.update.one($id(swiss.id), $inc("nbPlayers" -> -1)).void
          }
      }.void >>- recomputeAndUpdateAll(id)
    }

  def gameIdSource(
      swissId: Swiss.Id,
      batchSize: Int = 0,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): Source[Game.ID, _] =
    SwissPairing.fields { f =>
      colls.pairing.ext
        .find($doc(f.swissId -> swissId), $id(true))
        .sort($sort asc f.round)
        .batchSize(batchSize)
        .cursor[Bdoc](readPreference)
        .documentSource()
        .mapConcat(_.string("_id").toList)
    }

  def featuredInTeam(teamId: TeamID): Fu[List[Swiss]] =
    cache.featuredInTeam.get(teamId) flatMap { ids =>
      colls.swiss.byOrderedIds[Swiss, Swiss.Id](ids)(_.id)
    }

  def visibleInTeam(teamId: TeamID, nb: Int): Fu[List[Swiss]] =
    colls.swiss.ext.find($doc("teamId" -> teamId)).sort($sort desc "startsAt").list[Swiss](nb)

  def playerInfo(swiss: Swiss, userId: User.ID): Fu[Option[SwissPlayer.ViewExt]] =
    userRepo named userId flatMap {
      _ ?? { user =>
        colls.player.byId[SwissPlayer](SwissPlayer.makeId(swiss.id, user.id).value) flatMap {
          _ ?? { player =>
            SwissPairing.fields { f =>
              colls.pairing.ext
                .find($doc(f.swissId -> swiss.id, f.players -> player.userId))
                .sort($sort asc f.round)
                .list[SwissPairing]()
            } flatMap {
              pairingViews(_, player)
            } flatMap { pairings =>
              SwissPlayer.fields { f =>
                colls.player.countSel($doc(f.swissId -> swiss.id, f.score $gt player.score)).dmap(1.+)
              } map { rank =>
                val pairingMap = pairings.view.map { p =>
                  p.pairing.round -> p
                }.toMap
                SwissPlayer
                  .ViewExt(
                    player,
                    rank,
                    user.light,
                    pairingMap,
                    SwissSheet.one(swiss, pairingMap.view.mapValues(_.pairing).toMap, player)
                  )
                  .some
              }
            }
          }
        }
      }
    }

  def pairingViews(pairings: Seq[SwissPairing], player: SwissPlayer): Fu[Seq[SwissPairing.View]] =
    pairings.headOption ?? { first =>
      SwissPlayer.fields { f =>
        colls.player.ext
          .find($inIds(pairings.map(_ opponentOf player.userId).map { SwissPlayer.makeId(first.swissId, _) }))
          .list[SwissPlayer]()
      } flatMap { opponents =>
        lightUserApi asyncMany opponents.map(_.userId) map { users =>
          opponents.zip(users) map {
            case (o, u) => SwissPlayer.WithUser(o, u | LightUser.fallback(o.userId))
          }
        } map { opponents =>
          pairings flatMap { pairing =>
            opponents.find(_.player.userId == pairing.opponentOf(player.userId)) map {
              SwissPairing.View(pairing, _)
            }
          }
        }
      }
    }

  def searchPlayers(id: Swiss.Id, term: String, nb: Int): Fu[List[User.ID]] =
    User.couldBeUsername(term) ?? SwissPlayer.fields { f =>
      colls.player.primitive[User.ID](
        selector = $doc(
          f.swissId -> id,
          f.userId $startsWith term.toLowerCase
        ),
        sort = $sort desc f.score,
        nb = nb,
        field = f.userId
      )
    }

  def pageOf(swiss: Swiss, userId: User.ID): Fu[Option[Int]] =
    rankingApi(swiss) map {
      _ get userId map { rank =>
        (Math.floor(rank / 10) + 1).toInt
      }
    }

  private[swiss] def finishGame(game: Game): Funit =
    game.swissId.map(Swiss.Id) ?? { swissId =>
      Sequencing(swissId)(byId) { swiss =>
        if (!swiss.isStarted) {
          logger.info(s"Removing pairing ${game.id} finished after swiss ${swiss.id}")
          colls.pairing.delete.one($id(game.id)).void
        } else
          colls.pairing.byId[SwissPairing](game.id).dmap(_.filter(_.isOngoing)) flatMap {
            _ ?? { pairing =>
              colls.pairing
                .updateField(
                  $id(game.id),
                  SwissPairing.Fields.status,
                  game.winnerColor
                    .map(_.fold(pairing.white, pairing.black))
                    .fold[BSONValue](BSONNull)(BSONString.apply)
                )
                .void >> {
                if (swiss.nbOngoing > 0)
                  colls.swiss.update.one($id(swiss.id), $inc("nbOngoing" -> -1))
                else
                  fuccess {
                    logger.warn(s"swiss ${swiss.id} nbOngoing = ${swiss.nbOngoing}")
                  }
              } >>
                game.playerWhoDidNotMove.flatMap(_.userId).?? { absent =>
                  SwissPlayer.fields { f =>
                    colls.player
                      .updateField($doc(f.swissId -> swiss.id, f.userId -> absent), f.absent, true)
                      .void
                  }
                } >> {
                (swiss.nbOngoing <= 1) ?? {
                  if (swiss.round.value == swiss.settings.nbRounds) doFinish(swiss)
                  else if (swiss.settings.manualRounds) fuccess {
                    systemChat(swiss.id, s"Round ${swiss.round.value + 1} needs to be scheduled.")
                  }
                  else
                    colls.swiss
                      .updateField(
                        $id(swiss.id),
                        "nextRoundAt",
                        if (swiss.settings.oneDayInterval) game.createdAt plusDays 1
                        else DateTime.now.plusSeconds(swiss.settings.roundInterval.toSeconds.toInt)
                      )
                      .void >>-
                      systemChat(swiss.id, s"Round ${swiss.round.value + 1} will start soon.")
                }
              }
            }
          }
      } >> recomputeAndUpdateAll(swissId)
    }

  private[swiss] def destroy(swiss: Swiss): Funit =
    colls.swiss.delete.one($id(swiss.id)) >>
      colls.pairing.delete.one($doc(SwissPairing.Fields.swissId -> swiss.id)) >>
      colls.player.delete.one($doc(SwissPairing.Fields.swissId -> swiss.id)).void >>-
      socket.reload(swiss.id)

  private[swiss] def finish(oldSwiss: Swiss): Funit =
    Sequencing(oldSwiss.id)(startedById) { swiss =>
      colls.pairing.countSel($doc(SwissPairing.Fields.swissId -> swiss.id)) flatMap {
        case 0 => destroy(swiss)
        case _ => doFinish(swiss)
      }
    }
  private def doFinish(swiss: Swiss): Funit =
    SwissPlayer
      .fields { f =>
        colls.player.ext.find($doc(f.swissId -> swiss.id)).sort($sort desc f.score).one[SwissPlayer]
      }
      .flatMap { winner =>
        colls.swiss.update
          .one(
            $id(swiss.id),
            $unset("nextRoundAt", "featurable") ++ $set(
              "settings.nbRounds" -> swiss.round,
              "finishedAt"        -> DateTime.now,
              "winnerId"          -> winner.map(_.userId)
            )
          )
          .void
      } >>- {
      systemChat(swiss.id, s"Tournament completed!")
      socket.reload(swiss.id)
    }

  def kill(swiss: Swiss): Funit = {
    if (swiss.isStarted) finish(swiss)
    else if (swiss.isCreated) destroy(swiss)
    else funit
  } >>- cache.featuredInTeam.invalidate(swiss.teamId)

  private def recomputeAndUpdateAll(id: Swiss.Id): Funit =
    scoring(id).flatMap { res =>
      rankingApi.update(res)
      standingApi.update(res) >>
        boardApi.update(res)
    } >>- socket.reload(id)

  private[swiss] def startPendingRounds: Funit =
    colls.swiss.ext
      .find($doc("nextRoundAt" $lt DateTime.now), $id(true))
      .list[Bdoc](10)
      .map(_.flatMap(_.getAsOpt[Swiss.Id]("_id")))
      .flatMap { ids =>
        lila.common.Future.applySequentially(ids) { id =>
          Sequencing(id)(notFinishedById) { swiss =>
            if (swiss.nbPlayers >= 4)
              director.startRound(swiss).flatMap {
                _.fold {
                  systemChat(swiss.id, "All possible pairings were played.")
                  doFinish(swiss)
                } {
                  case (s, pairings) if s.nextRoundAt.isEmpty =>
                    systemChat(swiss.id, s"Round ${swiss.round.value + 1} started.")
                    funit
                  case (s, _) =>
                    systemChat(swiss.id, s"Round ${swiss.round.value + 1} failed.", true)
                    colls.swiss.update
                      .one($id(swiss.id), $set("nextRoundAt" -> DateTime.now.plusSeconds(61)))
                      .void
                }
              }
            else {
              if (swiss.startsAt isBefore DateTime.now.minusMinutes(60)) destroy(swiss)
              else {
                systemChat(swiss.id, "Not enough players for first round; delaying start.", true)
                colls.swiss.update
                  .one($id(swiss.id), $set("nextRoundAt" -> DateTime.now.plusSeconds(121)))
                  .void
              }
            }
          } >> recomputeAndUpdateAll(id)
        }
      }
      .monSuccess(_.swiss.tick)

  private[swiss] def checkOngoingGames: Funit =
    SwissPairing.fields { f =>
      colls.pairing.primitive[Game.ID]($doc(f.status -> SwissPairing.ongoing), f.id)
    } flatMap roundSocket.getGames flatMap { games =>
      val (finished, ongoing) = games.partition(_.finishedOrAborted)
      val flagged             = ongoing.filter(_ outoftime true)
      lila.mon.swiss.games("finished").record(finished.size)
      lila.mon.swiss.games("ongoing").record(ongoing.size)
      lila.mon.swiss.games("flagged").record(flagged.size)
      if (flagged.nonEmpty)
        Bus.publish(lila.hub.actorApi.map.TellMany(flagged.map(_.id), QuietFlag), "roundSocket")
      finished.map(finishGame).sequenceFu.void
    }

  private def systemChat(id: Swiss.Id, text: String, volatile: Boolean = false): Unit =
    chatApi.userChat.service(
      Chat.Id(id.value),
      text,
      _.Swiss,
      volatile
    )

  private def Sequencing[A: Zero](
      id: Swiss.Id
  )(fetch: Swiss.Id => Fu[Option[Swiss]])(run: Swiss => Fu[A]): Fu[A] =
    sequencer(id.value) {
      fetch(id) flatMap {
        _ ?? run
      }
    }
}
