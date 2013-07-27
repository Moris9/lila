import sbt._, Keys._
import play.Project._

object ApplicationBuild extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val lila = _root_.play.Project("lila", "4.0") settings (
    offline := true,
    libraryDependencies ++= Seq(
      scalaz, scalalib, hasher, config, apache, scalaTime,
      csv, jgit, actuarius, scalastic, findbugs, reactivemongo,
      playReactivemongo, spray.caching),
      scalacOptions := compilerOptions,
      sources in doc in Compile := List(),
      templatesImport ++= Seq(
        "lila.game.{ Game, Player, Pov }",
        "lila.user.{ User, Context }",
        "lila.security.Permission",
        "lila.app.templating.Environment._",
        "lila.common.paginator.Paginator")
  ) dependsOn api aggregate api

  lazy val modules = Seq(
    chess, common, db, user, security, wiki, hub, socket,
    message, notification, i18n, game, bookmark, search,
    gameSearch, timeline, forum, forumSearch, team, teamSearch,
    ai, analyse, mod, monitor, site, round, lobby, setup,
    importer, tournament, relation)

  lazy val moduleRefs = modules map projectToRef
  lazy val moduleCPDeps = moduleRefs map classpathDependency

  lazy val api = project("api", moduleCPDeps)
    .settings(
      libraryDependencies := provided(
        play.api, hasher, config, apache, csv, jgit,
        actuarius, scalastic, findbugs, reactivemongo)
    ) aggregate (moduleRefs: _*)

  lazy val common = project("common").settings(
    libraryDependencies ++= provided(play.api, play.test, reactivemongo, csv)
  )

  lazy val memo = project("memo", Seq(common)).settings(
    libraryDependencies ++= Seq(guava, findbugs, spray.caching) ++ provided(play.api)
  )

  lazy val db = project("db", Seq(common)).settings(
    libraryDependencies ++= provided(play.test, play.api, reactivemongo, playReactivemongo)
  )

  lazy val search = project("search", Seq(common, hub)).settings(
    libraryDependencies ++= provided(play.api, scalastic)
  )

  lazy val timeline = project("timeline", Seq(common, db, game, user, hub, security, relation)).settings(
    libraryDependencies ++= provided(
      play.api, play.test, reactivemongo, playReactivemongo)
  )

  lazy val mod = project("mod", Seq(common, db, user, hub, security)).settings(
    libraryDependencies ++= provided(
      play.api, play.test, reactivemongo, playReactivemongo)
  )

  lazy val user = project("user", Seq(common, memo, db, hub, chess)).settings(
    libraryDependencies ++= provided(
      play.api, play.test, reactivemongo, playReactivemongo, hasher)
  )

  lazy val game = project("game", Seq(common, memo, db, hub, user, chess)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo)
  )

  lazy val gameSearch = project("gameSearch", Seq(common, hub, chess, search, game, analyse)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo, scalastic)
  )

  lazy val analyse = project("analyse", Seq(common, hub, chess, game, user)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val round = project("round", Seq(
    common, db, memo, hub, socket, chess, game, user, security, i18n, ai)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo, playReactivemongo)
  )

  lazy val lobby = project("lobby", Seq(
    common, db, memo, hub, socket, chess, game, user, round, timeline, relation)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo, playReactivemongo)
  )

  lazy val setup = project("setup", Seq(
    common, db, memo, hub, socket, chess, game, user, lobby)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo, playReactivemongo)
  )

  lazy val importer = project("importer", Seq(common, chess, game, round)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo, playReactivemongo)
  )

  lazy val tournament = project("tournament", Seq(common, hub, socket, chess, game, round, setup, security)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo, playReactivemongo)
  )

  lazy val ai = project("ai", Seq(common, hub, chess, game, analyse)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val security = project("security", Seq(common, hub, db, user)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val relation = project("relation", Seq(common, db, memo, hub, user, game)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo, playReactivemongo)
  )

  lazy val message = project("message", Seq(common, db, user, hub, relation, security)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val forum = project("forum", Seq(common, db, user, security, hub, mod)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val forumSearch = project("forumSearch", Seq(common, hub, forum, search)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo, scalastic)
  )

  lazy val team = project("team", Seq(common, memo, db, user, forum, security, hub)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo)
  )

  lazy val teamSearch = project("teamSearch", Seq(common, hub, team, search)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo, scalastic)
  )

  lazy val i18n = project("i18n", Seq(common, db, user, hub)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo, jgit)
  )

  lazy val bookmark = project("bookmark", Seq(common, memo, db, hub, user, game)).settings(
    libraryDependencies ++= provided(
      play.api, play.test, reactivemongo, playReactivemongo)
  )

  lazy val wiki = project("wiki", Seq(common, db)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo, playReactivemongo, jgit, actuarius, guava)
  )

  lazy val notification = project("notification", Seq(common, user, hub)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val monitor = project("monitor", Seq(common, hub, socket, db)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo, playReactivemongo)
  )

  lazy val site = project("site", Seq(common, socket)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val socket = project("socket", Seq(common, hub, memo)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val hub = project("hub", Seq(common)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val chess = project("chess").settings(
    libraryDependencies ++= Seq(hasher)
  )
}
