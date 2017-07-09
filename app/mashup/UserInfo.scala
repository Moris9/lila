package lila.app
package mashup

import lila.api.Context
import lila.bookmark.BookmarkApi
import lila.forum.PostApi
import lila.game.Crosstable
import lila.relation.RelationApi
import lila.security.Granter
import lila.user.{ User, Trophy, Trophies, TrophyApi }

case class UserInfo(
    user: User,
    ranks: lila.rating.UserRankMap,
    nbPlaying: Int,
    hasSimul: Boolean,
    crosstable: Option[Crosstable.WithMatchup],
    nbBookmark: Int,
    nbImported: Int,
    ratingChart: Option[String],
    nbFollowers: Int,
    nbBlockers: Option[Int],
    nbPosts: Int,
    nbStudies: Int,
    playTime: Option[User.PlayTime],
    trophies: Trophies,
    teamIds: List[String],
    isStreamer: Boolean,
    isCoach: Boolean,
    insightVisible: Boolean,
    completionRate: Option[Double]
) {

  def nbRated = user.count.rated

  def nbWithMe = crosstable ?? (_.crosstable.nbGames)

  def percentRated: Int = math.round(nbRated / user.count.game.toFloat * 100)

  def completionRatePercent = completionRate.map { cr => math.round(cr * 100) }

  def isPublicMod = lila.security.Granter(_.PublicMod)(user)
  def isDeveloper = lila.security.Granter(_.Developer)(user)

  lazy val allTrophies = List(
    isPublicMod option Trophy(
      _id = "",
      user = user.id,
      kind = Trophy.Kind.Moderator,
      date = org.joda.time.DateTime.now
    ),
    isDeveloper option Trophy(
      _id = "",
      user = user.id,
      kind = Trophy.Kind.Developer,
      date = org.joda.time.DateTime.now
    ),
    isStreamer option Trophy(
      _id = "",
      user = user.id,
      kind = Trophy.Kind.Streamer,
      date = org.joda.time.DateTime.now
    )
  ).flatten ::: trophies

  def countTrophiesAndPerfCups = allTrophies.size + ranks.count(_._2 <= 100)
}

object UserInfo {

  def apply(
    bookmarkApi: BookmarkApi,
    relationApi: RelationApi,
    trophyApi: TrophyApi,
    gameCached: lila.game.Cached,
    crosstableApi: lila.game.CrosstableApi,
    postApi: PostApi,
    studyRepo: lila.study.StudyRepo,
    getRatingChart: User => Fu[Option[String]],
    getRanks: String => Fu[Map[String, Int]],
    isHostingSimul: String => Fu[Boolean],
    fetchIsStreamer: String => Fu[Boolean],
    fetchTeamIds: User.ID => Fu[List[String]],
    fetchIsCoach: User => Fu[Boolean],
    insightShare: lila.insight.Share,
    getPlayTime: User => Fu[Option[User.PlayTime]],
    completionRate: User.ID => Fu[Option[Double]]
  )(user: User, ctx: Context): Fu[UserInfo] =
    getRanks(user.id) zip
      (gameCached nbPlaying user.id) zip
      gameCached.nbImportedBy(user.id) zip
      (ctx.me.filter(user!=) ?? { me => crosstableApi.withMatchup(me.id, user.id) }) zip
      getRatingChart(user) zip
      relationApi.countFollowers(user.id) zip
      (ctx.me ?? Granter(_.UserSpy) ?? { relationApi.countBlockers(user.id) map (_.some) }) zip
      postApi.nbByUser(user.id) zip
      studyRepo.countByOwner(user.id) zip
      trophyApi.findByUser(user) zip
      fetchTeamIds(user.id) zip
      fetchIsCoach(user) zip
      fetchIsStreamer(user.id) zip
      (user.count.rated >= 10).??(insightShare.grant(user, ctx.me)) zip
      getPlayTime(user) zip
      completionRate(user.id) zip
      bookmarkApi.countByUser(user) flatMap {
        case ranks ~ nbPlaying ~ nbImported ~ crosstable ~ ratingChart ~ nbFollowers ~ nbBlockers ~ nbPosts ~ nbStudies ~ trophies ~ teamIds ~ isCoach ~ isStreamer ~ insightVisible ~ playTime ~ completionRate ~ nbBookmarks =>
          (nbPlaying > 0) ?? isHostingSimul(user.id) map { hasSimul =>
            new UserInfo(
              user = user,
              ranks = ranks,
              nbPlaying = nbPlaying,
              hasSimul = hasSimul,
              crosstable = crosstable,
              nbBookmark = nbBookmarks,
              nbImported = nbImported,
              ratingChart = ratingChart,
              nbFollowers = nbFollowers,
              nbBlockers = nbBlockers,
              nbPosts = nbPosts,
              nbStudies = nbStudies,
              playTime = playTime,
              trophies = trophies,
              teamIds = teamIds,
              isStreamer = isStreamer,
              isCoach = isCoach,
              insightVisible = insightVisible,
              completionRate = completionRate
            )
          }
      }
}
