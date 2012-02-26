import sbt._
import Keys._

trait Resolvers {
  val codahale = "repo.codahale.com" at "http://repo.codahale.com/"
  val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
  val typesafeS = "typesafe.com snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
  val iliaz = "iliaz.com" at "http://scala.iliaz.com/"
  val sonatype = "sonatype" at "http://oss.sonatype.org/content/repositories/releases"
  val sonatypeS = "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
}

trait Dependencies {
  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"
  val specs2 = "org.specs2" %% "specs2" % "1.8.2"
  val redis = "net.debasishg" %% "redisclient" % "2.4.2"
  val json = "net.liftweb" %% "lift-json" % "2.4-RC1"
  val casbah = "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
  val salat = "com.novus" %% "salat-core" % "0.0.8-SNAPSHOT"
  val slf4jNop = "org.slf4j" % "slf4j-nop" % "1.6.4"
  val instrumenter = "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0"
  val gson = "com.google.code.gson" % "gson" % "1.7.1"
}

object ApplicationBuild extends Build with Resolvers with Dependencies {

  lazy val chess = Project("chess", file("chess"), settings = Project.defaultSettings).settings(
    libraryDependencies in test := Seq(specs2),
    resolvers := Seq(codahale, sonatype),
    shellPrompt := ShellPrompt.buildShellPrompt,
    scalacOptions := Seq("-deprecation", "-unchecked")
  ) dependsOn (ornicarScalalib)

  lazy val http = Project("http", file("http"), settings = Project.defaultSettings).settings(
    libraryDependencies := Seq(scalaz, specs2, redis, json, casbah, salat),
    resolvers := Seq(codahale, typesafe, typesafeS, iliaz, sonatype),
    shellPrompt := ShellPrompt.buildShellPrompt,
    scalacOptions := Seq("-deprecation", "-unchecked")
  ) dependsOn (chess, ornicarScalalib)

  lazy val benchmark = Project("benchmark", file("benchmark"), settings = Project.defaultSettings).settings(
    fork in run := true,
    libraryDependencies := Seq(instrumenter, gson),
    resolvers := Seq(codahale, sonatype),
    shellPrompt := ShellPrompt.buildShellPrompt,
    scalacOptions := Seq("-deprecation", "-unchecked"),
    // we need to add the runtime classpath as a "-cp" argument
    // to the `javaOptions in run`, otherwise caliper
    // will not see the right classpath and die with a ConfigurationException
    // unfortunately `javaOptions` is a SettingsKey and
    // `fullClasspath in Runtime` is a TaskKey, so we need to
    // jump through these hoops here in order to
    // feed the result of the latter into the former
    onLoad in Global ~= { previous => state =>
      previous {
        state get key match {
          case None =>
            // get the runtime classpath, turn into a colon-delimited string
            val classPath = Project.runTask(fullClasspath in Runtime, state).get._2.toEither.right.get.files.mkString(":")
            // return a state with javaOptionsPatched = true and javaOptions set correctly
            Project.extract(state).append(Seq(javaOptions in run ++= Seq("-cp", classPath)), state.put(key, true))
          case Some(_) => state // the javaOptions are already patched
        }
      }
    }
  ) dependsOn (chess)

  lazy val ornicarScalalib = uri("git://github.com/ornicar/scalalib#1.6")

  // attribute key to prevent circular onLoad hook
  val key = AttributeKey[Boolean]("javaOptionsPatched")
}

object ShellPrompt {
  val buildShellPrompt =
    (state: State) ⇒ "%s> ".format(Project.extract(state).currentProject.id)
}
