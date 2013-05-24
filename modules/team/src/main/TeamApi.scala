package lila.team

import org.scala_tools.time.Imports._

import actorApi._
import lila.db.api._
import lila.hub.actorApi.forum.MakeTeam
import lila.hub.actorApi.timeline.{ Propagate, TeamJoin, TeamCreate }
import lila.hub.ActorLazyRef
import lila.user.tube.userTube
import lila.user.{ User, Context }
import tube._

final class TeamApi(
    cached: Cached,
    notifier: Notifier,
    forum: ActorLazyRef,
    indexer: ActorLazyRef,
    timeline: ActorLazyRef) {

  val creationPeriod = 1.week

  def team(id: String) = $find.byId[Team](id)

  def request(id: String) = $find.byId[Request](id)

  def create(setup: TeamSetup, me: User): Option[Fu[Team]] = me.canTeam option {
    val s = setup.trim
    val team = Team.make(
      name = s.name,
      location = s.location,
      description = s.description,
      open = s.isOpen,
      createdBy = me)
    $insert(team) >>
      MemberRepo.add(team.id, me.id) >>
      (cached.teamIds remove me.id) >>-
      (forum ! MakeTeam(team.id, team.name)) >>-
      (indexer ! InsertTeam(team)) >>-
      (timeline ! Propagate(
        TeamCreate(me.id, team.id)
      ).toFriendsOf(me.id)) inject team
  }

  def update(team: Team, edit: TeamEdit, me: User): Funit = edit.trim |> { e ⇒
    team.copy(
      location = e.location,
      description = e.description,
      open = e.isOpen
    ) |> { team ⇒ $update(team) >>- (indexer ! InsertTeam(team)) }
  }

  def mine(me: User): Fu[List[Team]] =
    cached teamIds me.id flatMap $find.byOrderedIds[Team]

  def hasCreatedRecently(me: User): Fu[Boolean] =
    TeamRepo.userHasCreatedSince(me.id, creationPeriod)

  def requestsWithUsers(team: Team): Fu[List[RequestWithUser]] = for {
    requests ← RequestRepo findByTeam team.id
    users ← $find.byOrderedIds[User](requests map (_.user))
  } yield requests zip users map {
    case (request, user) ⇒ RequestWithUser(request, user)
  }

  def requestsWithUsers(user: User): Fu[List[RequestWithUser]] = for {
    teamIds ← TeamRepo teamIdsByCreator user.id
    requests ← RequestRepo findByTeams teamIds
    users ← $find.byOrderedIds[User](requests map (_.user))
  } yield requests zip users map {
    case (request, user) ⇒ RequestWithUser(request, user)
  }

  def join(teamId: String)(implicit ctx: Context): Fu[Option[Requesting]] = for {
    teamOption ← $find.byId[Team](teamId)
    result ← ~(teamOption |@| ctx.me.filter(_.canTeam))({
      case (team, user) if team.open ⇒
        (doJoin(team, user.id) inject Joined(team).some): Fu[Option[Requesting]]
      case (team, user) ⇒
        fuccess(Motivate(team).some: Option[Requesting])
    })
  } yield result

  def requestable(teamId: String, user: User): Fu[Option[Team]] = for {
    teamOption ← $find.byId[Team](teamId)
    able ← teamOption.??(requestable(_, user))
  } yield teamOption filter (_ ⇒ able)

  def requestable(team: Team, user: User): Fu[Boolean] =
    RequestRepo.exists(team.id, user.id) zip belongsTo(team.id, user.id) map {
      case (false, false) ⇒ true
      case _              ⇒ false
    }

  def createRequest(team: Team, setup: RequestSetup, user: User): Funit =
    requestable(team, user) flatMap {
      _ ?? {
        val request = Request.make(team = team.id, user = user.id, message = setup.message)
        val rwu = RequestWithUser(request, user)
        $insert(request) >> (cached.nbRequests remove team.createdBy)
      }
    }

  def processRequest(team: Team, request: Request, accept: Boolean): Funit = for {
    _ ← $remove(request)
    _ ← cached.nbRequests remove team.createdBy
    userOption ← $find.byId[User](request.user)
    _ ← userOption.filter(_ ⇒ accept).??(user ⇒
      doJoin(team, user.id) >>- notifier.acceptRequest(team, request)
    )
  } yield ()

  def doJoin(team: Team, userId: String): Funit =
    belongsTo(team.id, userId) flatMap { belongs ⇒
      (!belongs) ?? {
        MemberRepo userIdsByTeam team.id flatMap { previousMembers ⇒
          MemberRepo.add(team.id, userId) >>
            TeamRepo.incMembers(team.id, +1) >>
            (cached.teamIds remove userId) >>-
            (timeline ! Propagate(
              TeamJoin(userId, team.id)
            ).toFriendsOf(userId).toUsers(previousMembers))
        }
      }
    }

  def quit(teamId: String)(implicit ctx: Context): Fu[Option[Team]] = for {
    teamOption ← $find.byId[Team](teamId)
    result ← ~(teamOption |@| ctx.me)({
      case (team, user) ⇒ doQuit(team, user.id) inject team.some
    })
  } yield result

  def doQuit(team: Team, userId: String): Funit =
    belongsTo(team.id, userId) flatMap {
      _ ?? {
        MemberRepo.remove(team.id, userId) >>
          TeamRepo.incMembers(team.id, -1) >>
          (cached.teamIds remove userId)
      }
    }

  def quitAll(userId: String): Funit = MemberRepo.removeByUser(userId)

  def kick(team: Team, userId: String): Funit = doQuit(team, userId)

  def enable(team: Team): Funit =
    TeamRepo.enable(team) >>- (indexer ! InsertTeam(team))

  def disable(team: Team): Funit =
    TeamRepo.disable(team) >>- (indexer ! RemoveTeam(team.id))

  //   // delete for ever, with members but not forums
  def delete(team: Team): Funit =
    $remove(team) >>
      MemberRepo.removeByteam(team.id) >>-
      (indexer ! RemoveTeam(team.id))

  def belongsTo(teamId: String, userId: String): Fu[Boolean] =
    cached teamIds userId map (_ contains teamId)

  def teamIds(userId: String) = cached teamIds userId

  def teamName(teamId: String) = cached name teamId

  def nbRequests(teamId: String) = cached nbRequests teamId
}
