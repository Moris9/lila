package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._

import Client.Skill
import lila.db.Implicits._
import lila.hub.{ actorApi => hubApi }

final class FishnetApi(
    hub: lila.hub.Env,
    moveColl: Coll,
    analysisColl: Coll,
    clientColl: Coll,
    sequencer: Sequencer,
    saveAnalysis: lila.analyse.Analysis => Funit,
    offlineMode: Boolean) {

  import BSONHandlers._

  def authenticateClient(req: JsonApi.Request) = {
    if (offlineMode) repo.getOfflineClient
    else repo.getEnabledClient(req.fishnet.apikey)
  } flatMap {
    _ ?? { client =>
      repo.updateClientInstance(client, req.instance) map some
    }
  }

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = client.skill match {
    case Skill.Move     => acquireMove(client)
    case Skill.Analysis => acquireAnalysis(client)
    case Skill.All      => acquireMove(client) orElse acquireAnalysis(client)
  }

  private def acquireMove(client: Client): Fu[Option[JsonApi.Work]] = sequencer.move {
    moveColl.find(BSONDocument(
      "acquired" -> BSONDocument("$exists" -> false)
    )).sort(BSONDocument("createdAt" -> 1)).one[Work.Move].flatMap {
      _ ?? { work =>
        repo.updateMove(work assignTo client) zip repo.updateClient(client acquire work) inject work.some
      }
    }
  } map { _ map JsonApi.fromWork }

  private def acquireAnalysis(client: Client): Fu[Option[JsonApi.Work]] = sequencer.analysis {
    analysisColl.find(BSONDocument(
      "acquired" -> BSONDocument("$exists" -> false)
    )).sort(BSONDocument(
      "sender.system" -> 1, // user requests first, then lichess auto analysis
      "createdAt" -> 1 // oldest requests first
    )).one[Work.Analysis].flatMap {
      _ ?? { work =>
        repo.updateAnalysis(work assignTo client) zip repo.updateClient(client acquire work) inject work.some
      }
    }
  } map { _ map JsonApi.fromWork }

  def postMove(workId: Work.Id, client: Client, data: JsonApi.Request.PostMove): Funit = sequencer.move {
    repo.getMove(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      case None =>
        log.warn(s"Received unknown or unacquired move $workId by ${client.fullId}")
        funit
      case Some(work) => data.move.uci match {
        case Some(uci) =>
          hub.actor.roundMap ! hubApi.map.Tell(work.game.id, hubApi.round.FishnetPlay(uci, work.currentFen))
          repo.deleteMove(work) zip
            repo.updateClient(client success work) void
        case _ =>
          log.warn(s"Received invalid move ${data.move} by ${client.fullId}")
          repo.updateOrGiveUpMove(work.invalid) zip repo.updateClient(client invalid work) void
      }
    }
  }

  def postAnalysis(workId: Work.Id, client: Client, data: JsonApi.Request.PostAnalysis): Funit = sequencer.analysis {
    repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      case None =>
        log.warn(s"Received unknown or unacquired analysis $workId by ${client.fullId}")
        fuccess(none)
      case Some(work) => AnalysisBuilder(client, work, data.pp).thenPp flatMap { analysis =>
        repo.deleteAnalysis(work) >>
          repo.updateClient(client success work) inject analysis.some
      } recoverWith {
        case e: Exception =>
          log.warn(s"Received invalid analysis $workId by ${client.fullId}: ${e.getMessage}")
          repo.updateOrGiveUpAnalysis(work.invalid) zip
            repo.updateClient(client invalid work) inject none
      }
    }
  } flatMap { _ ?? saveAnalysis }

  private[fishnet] def createClient(key: Client.Key, userId: Client.UserId, skill: String) =
    Client.Skill.byKey(skill).fold(fufail[Unit](s"Invalid skill $skill")) { sk =>
      clientColl.insert(Client(
        _id = key,
        userId = userId,
        skill = sk,
        instance = None,
        enabled = true,
        stats = Stats.empty,
        createdAt = DateTime.now)).void
    }
  private[fishnet] def setClientSkill(key: Client.Key, skill: String) =
    Client.Skill.byKey(skill).fold(fufail[Unit](s"Invalid skill $skill")) { sk =>
      clientColl.update(repo.selectClient(key), BSONDocument("$set" -> BSONDocument("skill" -> sk.key))).void
    }

  private[fishnet] object repo {

    def getClient(key: Client.Key) = clientColl.find(selectClient(key)).one[Client]
    def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
    def getOfflineClient = getEnabledClient(Client.offline.key) orElse fuccess(Client.offline.some)
    def updateClient(client: Client): Funit = clientColl.update(selectClient(client.key), client, upsert = true).void
    def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
      client.updateInstance(instance).fold(fuccess(client)) { updated =>
        updateClient(updated) inject updated
      }
    def deleteClient(key: Client.Key) = clientColl.remove(selectClient(key))
    def enableClient(key: Client.Key, v: Boolean): Funit =
      clientColl.update(selectClient(key), BSONDocument("$set" -> BSONDocument("enabled" -> v))).void

    def addMove(move: Work.Move) = moveColl.insert(move).void
    def getMove(id: Work.Id) = moveColl.find(selectWork(id)).one[Work.Move]
    def updateMove(move: Work.Move) = moveColl.update(selectWork(move.id), move).void
    def deleteMove(move: Work.Move) = moveColl.remove(selectWork(move.id)).void
    def giveUpMove(move: Work.Move) = deleteMove(move) >>- log.warn(s"Give up on move $move")
    def updateOrGiveUpMove(move: Work.Move) = if (move.isOutOfTries) giveUpMove(move) else updateMove(move)

    def addAnalysis(ana: Work.Analysis) = analysisColl.insert(ana).void
    def getAnalysis(id: Work.Id) = analysisColl.find(selectWork(id)).one[Work.Analysis]
    def updateAnalysis(ana: Work.Analysis) = analysisColl.update(selectWork(ana.id), ana).void
    def deleteAnalysis(ana: Work.Analysis) = analysisColl.remove(selectWork(ana.id)).void
    def giveUpAnalysis(ana: Work.Analysis) = deleteAnalysis(ana) >>- log.warn(s"Give up on analysis $ana")
    def updateOrGiveUpAnalysis(ana: Work.Analysis) = if (ana.isOutOfTries) giveUpAnalysis(ana) else updateAnalysis(ana)

    def similarMoveExists(work: Work.Move): Fu[Boolean] = moveColl.count(BSONDocument(
      "game.id" -> work.game.id,
      "currentFen" -> work.currentFen
    ).some) map (0 !=)

    def getSimilarAnalysis(work: Work.Analysis): Fu[Option[Work.Analysis]] =
      analysisColl.find(BSONDocument("game.id" -> work.game.id)).one[Work.Analysis]

    def selectWork(id: Work.Id) = BSONDocument("_id" -> id.value)
    def selectClient(key: Client.Key) = BSONDocument("_id" -> key.value)
  }
}
