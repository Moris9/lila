package lila
package game

import memo._
import model._
import socket._
import chess.Color

import play.api.libs.concurrent._
import play.api.Play.current
import akka.actor.{ ActorRef, Props, PoisonPill }

import scalaz.effects._
import scala.collection.JavaConversions._

final class HubMemo(
  makeHistory: () ⇒ History,
  timeout: Int
) {

  private val cache = {
    import com.google.common.cache._
    import memo.Builder._
    CacheBuilder.newBuilder()
      .asInstanceOf[CacheBuilder[String, ActorRef]]
      .removalListener(onRemove _)
      .build[String, ActorRef](compute _)
  }

  def all: Map[String, ActorRef] = cache.asMap.toMap

  def hubs: List[ActorRef] = cache.asMap.toMap.values.toList

  def get(gameId: String): ActorRef = cache get gameId

  def getIfPresent(gameId: String): Option[ActorRef] = Option {
    cache getIfPresent gameId
  }

  def getFromFullId(fullId: String): ActorRef = get(DbGame takeGameId fullId)

  def getIfPresentFromFullId(fullId: String): Option[ActorRef] =
    getIfPresent(DbGame takeGameId fullId)

  def remove(gameId: String): IO[Unit] = io {
    cache invalidate gameId
  }

  def count = cache.size

  private def compute(gameId: String): ActorRef = {
    Akka.system.actorOf(Props(new Hub(
      gameId = gameId,
      history = makeHistory(),
      timeout = timeout
    )), name = "game_hub_" + gameId)
  }

  private def onRemove(gameId: String, actor: ActorRef) {
    actor ! Close
  }
}
