package lila.tournament

import lila.db.api._
import lila.db.Implicits._
import tube.tournamentTube

import play.api.libs.json._
import org.scala_tools.time.Imports._

object TournamentRepo {

  private def asCreated(tour: Tournament): Option[Created] = tour.some collect {
    case t: Created ⇒ t
  }
  private def asStarted(tour: Tournament): Option[Started] = tour.some collect {
    case t: Started ⇒ t
  }
  private def asFinished(tour: Tournament): Option[Finished] = tour.some collect {
    case t: Finished ⇒ t
  }

  def byId(id: String): Fu[Option[Tournament]] = $find byId id

  def createdById(id: String): Fu[Option[Created]] = byIdAs(id, asCreated)

  def startedById(id: String): Fu[Option[Started]] = byIdAs(id, asStarted)

  def createdByIdAndCreator(id: String, userId: String): Fu[Option[Created]] =
    createdById(id) map (_ filter (_ isCreator userId))

  private def byIdAs[A <: Tournament](id: String, as: Tournament ⇒ Option[A]): Fu[Option[A]] =
    $find byId id map (_ flatMap as)

  def created: Fu[List[Created]] = $find(
    $query(Json.obj("status" -> Status.Created.id)) sort $sort.createdDesc
  ) map { _.map(asCreated).flatten }

  def started: Fu[List[Started]] = $find(
    $query(Json.obj("status" -> Status.Started.id)) sort $sort.createdDesc
  ) map { _.map(asStarted).flatten }

  def finished(limit: Int): Fu[List[Finished]] = $find(
    $query(Json.obj("status" -> Status.Finished.id)) sort $sort.createdDesc,
    limit
  ) map { _.map(asFinished).flatten }

  def withdraw(userId: String): Fu[List[String]] = for {
    createds ← created
    createdIds ← (createds map (_ withdraw userId) collect {
      case scalaz.Success(tour) ⇒ $update(tour: Tournament) inject tour.id
    }).sequence
    starteds ← started
    startedIds ← (starteds map (_ withdraw userId) collect {
      case scalaz.Success(tour) ⇒ $update(tour: Tournament) inject tour.id
    }).sequence
  } yield createdIds ::: startedIds
}
