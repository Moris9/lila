import sbt._
import Keys._

object Dependencies {

  private val home = "file://" + Path.userHome.absolutePath

  object Resolvers {
    val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
    val typesafeS = "typesafe.com" at "http://repo.typesafe.com/typesafe/snapshots/"
    val iliaz = "iliaz.com" at "http://scala.iliaz.com/"
    val sonatype = "sonatype" at "http://oss.sonatype.org/content/repositories/releases"
    val sonatypeS = "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
    val t2v = "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
    val jgitMaven = "jgit-maven" at "http://download.eclipse.org/jgit/maven"
    val awesomepom = "awesomepom" at "https://raw.github.com/jibs/maven-repo-scala/master"
    val sprayRepo = "spray repo" at "http://repo.spray.io"
    val localSonatype = "local sonatype repo" at home + "/local-repo/sonatype/snapshots"
    val local = "local repo" at home + "/local-repo"
    val roundeights = "RoundEights" at "http://maven.spikemark.net/roundeights"

    val commons = Seq(
      local,
      // localSonatype,
      // sonatypeS,
      sonatype,
      awesomepom, iliaz,
      typesafe, 
      roundeights,
      // typesafeS,
      t2v, jgitMaven, sprayRepo)
  }

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.0.4"
  val scalalib = "com.github.ornicar" %% "scalalib" % "4.22"
  val config = "com.typesafe" % "config" % "1.0.2"
  val apache = "org.apache.commons" % "commons-lang3" % "3.1"
  val scalaTime = "com.github.nscala-time" %% "nscala-time" % "0.6.0"
  val guava = "com.google.guava" % "guava" % "15.0"
  val findbugs = "com.google.code.findbugs" % "jsr305" % "2.0.1"
  val csv = "com.github.tototoshi" %% "scala-csv" % "0.8.0"
  val hasher = "com.roundeights" %% "hasher" % "1.0.0"
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "2.3.1.201302201838-r"
  val actuarius = "eu.henkelmann" %% "actuarius" % "0.2.6-THIB"
  val jodaTime = "joda-time" % "joda-time" % "2.3"
  val scalastic = "org.scalastic" %% "scalastic" % "0.90.3"
  val RM = "org.reactivemongo" %% "reactivemongo" % "0.10.1-PRISMIC"
  val PRM = "org.reactivemongo" %% "play2-reactivemongo" % "0.10.1-PRISMIC"
  object play {
    val version = "2.2.1"
    val api = "com.typesafe.play" %% "play" % version
    val test = "com.typesafe.play" %% "play-test" % version
  }
  object spray {
    val version = "1.2.0"
    val caching = "io.spray" % "spray-caching" % version
    val util = "io.spray" % "spray-util" % version
  }
}
