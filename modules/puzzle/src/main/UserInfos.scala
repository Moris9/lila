package lila.puzzle

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

case class UserInfos(user: User, history: List[Round])

final class UserInfosApi(roundColl: Coll, currentPuzzleId: User => Fu[Option[PuzzleId]]) {

  private val historySize = 15
  private val chartSize = 15

  def apply(user: Option[User]): Fu[Option[UserInfos]] = user ?? { apply(_) map (_.some) }

  def apply(user: User): Fu[UserInfos] = for {
    current <- currentPuzzleId(user)
    rounds <- fetchRounds(user.id, current)
  } yield new UserInfos(user, rounds)

  private def fetchRounds(userId: User.ID, currentPuzzleId: Option[PuzzleId]): Fu[List[Round]] =
    roundColl.find(
      $doc(Round.BSONFields.id $startsWith s"$userId:") ++
        currentPuzzleId.??(id => $doc(Round.BSONFields.id $lte s"$userId:$id"))
    ).sort($sort desc Round.BSONFields.id)
      .cursor[Round]()
      .gather[List](historySize atLeast chartSize)
      .map(_.reverse)
}
