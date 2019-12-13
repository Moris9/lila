package lila.fishnet

import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

final private class FishnetRepo(
    analysisColl: Coll,
    clientColl: Coll,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import BSONHandlers._

  private val clientCache = asyncCache.clearable[Client.Key, Option[Client]](
    name = "fishnet.client",
    f = key => clientColl.one[Client](selectClient(key)),
    expireAfter = _.ExpireAfterWrite(5 minutes)
  )

  def getClient(key: Client.Key)        = clientCache get key
  def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
  def getOfflineClient: Fu[Client]      = getEnabledClient(Client.offline.key) getOrElse fuccess(Client.offline)
  def updateClient(client: Client): Funit =
    clientColl.update.one(selectClient(client.key), client, upsert = true).void >>- clientCache.invalidate(
      client.key
    )
  def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
    client.updateInstance(instance).fold(fuccess(client)) { updated =>
      updateClient(updated) inject updated
    }
  def addClient(client: Client)     = clientColl.insert.one(client)
  def deleteClient(key: Client.Key) = clientColl.delete.one(selectClient(key)) >>- clientCache.invalidate(key)
  def enableClient(key: Client.Key, v: Boolean): Funit =
    clientColl.update.one(selectClient(key), $set("enabled" -> v)).void >>- clientCache.invalidate(key)
  def allRecentClients =
    clientColl.ext
      .find(
        $doc(
          "instance.seenAt" $gt Client.Instance.recentSince
        )
      )
      .cursor[Client]()
      .gather[List]()
  def lichessClients =
    clientColl.ext
      .find(
        $doc(
          "enabled" -> true,
          "userId" $startsWith "lichess-"
        )
      )
      .cursor[Client]()
      .gather[List]()

  def addAnalysis(ana: Work.Analysis)    = analysisColl.insert.one(ana).void
  def getAnalysis(id: Work.Id)           = analysisColl.ext.find(selectWork(id)).one[Work.Analysis]
  def updateAnalysis(ana: Work.Analysis) = analysisColl.update.one(selectWork(ana.id), ana).void
  def deleteAnalysis(ana: Work.Analysis) = analysisColl.delete.one(selectWork(ana.id)).void
  def giveUpAnalysis(ana: Work.Analysis) = deleteAnalysis(ana) >>- logger.warn(s"Give up on analysis $ana")
  def updateOrGiveUpAnalysis(ana: Work.Analysis) =
    if (ana.isOutOfTries) giveUpAnalysis(ana) else updateAnalysis(ana)
  def countAnalysis(acquired: Boolean) = analysisColl.countSel($doc("acquired" $exists acquired))
  def countUserAnalysis                = analysisColl.countSel($doc("sender.system" -> false))

  def getSimilarAnalysis(work: Work.Analysis): Fu[Option[Work.Analysis]] =
    analysisColl.one[Work.Analysis]($doc("game.id" -> work.game.id))

  def selectWork(id: Work.Id)       = $id(id.value)
  def selectClient(key: Client.Key) = $id(key.value)

  private[fishnet] def toKey(keyOrUser: String): Fu[Client.Key] =
    clientColl.primitiveOne[String](
      $or(
        "_id" $eq keyOrUser,
        "userId" $eq keyOrUser
      ),
      "_id"
    ) orFail "client not found" map Client.Key.apply
}
