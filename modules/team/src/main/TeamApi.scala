package lila.team

import actorApi._
import akka.actor.ActorSelection
import lila.db.dsl._
import lila.hub.actorApi.team.{ CreateTeam, JoinTeam }
import lila.hub.actorApi.timeline.{ Propagate, TeamJoin, TeamCreate }
import lila.user.{ User, UserRepo, UserContext }
import org.joda.time.Period
import reactivemongo.api.Cursor

final class TeamApi(
    coll: Colls,
    cached: Cached,
    notifier: Notifier,
    bus: lila.common.Bus,
    indexer: ActorSelection,
    timeline: ActorSelection
) {

  import BSONHandlers._

  val creationPeriod = Period weeks 1

  def team(id: String) = coll.team.byId[Team](id)

  def request(id: String) = coll.request.byId[Request](id)

  def create(setup: TeamSetup, me: User): Option[Fu[Team]] = me.canTeam option {
    val s = setup.trim
    val team = Team.make(
      name = s.name,
      location = s.location,
      description = s.description,
      open = s.isOpen,
      createdBy = me
    )
    coll.team.insert(team) >>
      MemberRepo.add(team.id, me.id) >>- {
        cached invalidateTeamIds me.id
        indexer ! InsertTeam(team)
        timeline ! Propagate(
          TeamCreate(me.id, team.id)
        ).toFollowersOf(me.id)
        bus.publish(CreateTeam(id = team.id, name = team.name, userId = me.id), 'team)
      } inject team
  }

  def update(team: Team, edit: TeamEdit, me: User): Funit = edit.trim |> { e =>
    team.copy(
      location = e.location,
      description = e.description,
      open = e.isOpen
    ) |> { team =>
      coll.team.update($id(team.id), team).void >>- (indexer ! InsertTeam(team))
    }
  }

  def mine(me: User): Fu[List[Team]] =
    cached teamIds me.id flatMap { ids => coll.team.byIds[Team](ids.toArray) }

  def hasTeams(me: User): Fu[Boolean] = cached.teamIds(me.id).map(_.value.nonEmpty)

  def hasCreatedRecently(me: User): Fu[Boolean] =
    TeamRepo.userHasCreatedSince(me.id, creationPeriod)

  def requestsWithUsers(team: Team): Fu[List[RequestWithUser]] = for {
    requests ← RequestRepo findByTeam team.id
    users ← UserRepo usersFromSecondary requests.map(_.user)
  } yield requests zip users map {
    case (request, user) => RequestWithUser(request, user)
  }

  def requestsWithUsers(user: User): Fu[List[RequestWithUser]] = for {
    teamIds ← TeamRepo teamIdsByCreator user.id
    requests ← RequestRepo findByTeams teamIds
    users ← UserRepo usersFromSecondary requests.map(_.user)
  } yield requests zip users map {
    case (request, user) => RequestWithUser(request, user)
  }

  def join(teamId: String)(implicit ctx: UserContext): Fu[Option[Requesting]] = for {
    teamOption ← coll.team.byId[Team](teamId)
    result ← ~(teamOption |@| ctx.me.filter(_.canTeam))({
      case (team, user) if team.open =>
        (doJoin(team, user.id) inject Joined(team).some): Fu[Option[Requesting]]
      case (team, user) =>
        fuccess(Motivate(team).some: Option[Requesting])
    })
  } yield result

  def requestable(teamId: String, user: User): Fu[Option[Team]] = for {
    teamOption ← coll.team.byId[Team](teamId)
    able ← teamOption.??(requestable(_, user))
  } yield teamOption filter (_ => able)

  def requestable(team: Team, user: User): Fu[Boolean] = for {
    belongs <- belongsTo(team.id, user.id)
    requested <- RequestRepo.exists(team.id, user.id)
  } yield !belongs && !requested

  def createRequest(team: Team, setup: RequestSetup, user: User): Funit =
    requestable(team, user) flatMap {
      _ ?? {
        val request = Request.make(team = team.id, user = user.id, message = setup.message)
        coll.request.insert(request).void >>- (cached.nbRequests invalidate team.createdBy)
      }
    }

  def processRequest(team: Team, request: Request, accept: Boolean): Funit = for {
    _ ← coll.request.remove(request)
    _ = cached.nbRequests invalidate team.createdBy
    userOption ← UserRepo byId request.user
    _ ← userOption.filter(_ => accept).??(user =>
      doJoin(team, user.id) >>- notifier.acceptRequest(team, request))
  } yield ()

  def deleteRequestsByUserId(userId: lila.user.User.ID) =
    RequestRepo.getByUserId(userId) flatMap {
      _.map { request =>
        RequestRepo.remove(request.id) >>
          TeamRepo.creatorOf(request.team).map { _ ?? cached.nbRequests.invalidate }
      }.sequenceFu
    }

  def doJoin(team: Team, userId: String): Funit = !belongsTo(team.id, userId) flatMap {
    _ ?? {
      MemberRepo.add(team.id, userId) >>
        TeamRepo.incMembers(team.id, +1) >>- {
          cached invalidateTeamIds userId
          timeline ! Propagate(TeamJoin(userId, team.id)).toFollowersOf(userId)
          bus.publish(JoinTeam(id = team.id, userId = userId), 'team)
        }
    } recover lila.db.recoverDuplicateKey(_ => ())
  }

  def quit(teamId: String)(implicit ctx: UserContext): Fu[Option[Team]] = for {
    teamOption ← coll.team.byId[Team](teamId)
    result ← ~(teamOption |@| ctx.me)({
      case (team, user) => doQuit(team, user.id) inject team.some
    })
  } yield result

  def doQuit(team: Team, userId: String): Funit = belongsTo(team.id, userId) flatMap {
    _ ?? {
      MemberRepo.remove(team.id, userId) >>
        TeamRepo.incMembers(team.id, -1) >>-
        (cached invalidateTeamIds userId)
    }
  }

  def quitAll(userId: String): Funit = MemberRepo.removeByUser(userId)

  def kick(team: Team, userId: String): Funit = doQuit(team, userId)

  def enable(team: Team): Funit =
    TeamRepo.enable(team).void >>- (indexer ! InsertTeam(team))

  def disable(team: Team): Funit =
    TeamRepo.disable(team).void >>- (indexer ! RemoveTeam(team.id))

  // delete for ever, with members but not forums
  def delete(team: Team): Funit =
    coll.team.remove($id(team.id)) >>
      MemberRepo.removeByteam(team.id) >>-
      (indexer ! RemoveTeam(team.id))

  def syncBelongsTo(teamId: String, userId: String): Boolean =
    cached.syncTeamIds(userId) contains teamId

  def belongsTo(teamId: String, userId: String): Fu[Boolean] =
    cached.teamIds(userId) map (_ contains teamId)

  def owns(teamId: String, userId: String): Fu[Boolean] =
    TeamRepo ownerOf teamId map (Some(userId) ==)

  def teamName(teamId: String) = cached name teamId

  def nbRequests(teamId: String) = cached.nbRequests get teamId

  def recomputeNbMembers =
    coll.team.find($empty).cursor[Team]().foldWhileM({}) { (_, team) =>
      for {
        nb <- MemberRepo.countByTeam(team.id)
        _ <- coll.team.updateField($id(team.id), "nbMembers", nb)
      } yield Cursor.Cont({})
    }
}
