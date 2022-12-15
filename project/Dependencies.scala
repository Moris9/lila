import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {
  val arch = if (System.getProperty("os.arch").toLowerCase.startsWith("aarch")) "aarch_64" else "x86_64"
  val (os, notifier) =
    if (System.getProperty("os.name").toLowerCase.startsWith("mac"))
      ("osx", "kqueue")
    else
      ("linux", "epoll")

  val lilaMaven = "lila-maven" at "https://raw.githubusercontent.com/lichess-org/lila-maven/master"
  val sonashots = "sonashots" at "https://oss.sonatype.org/content/repositories/snapshots"

  val cats        = "org.typelevel"                %% "cats-core"                       % "2.9.0"
  val alleycats   = "org.typelevel"                %% "alleycats-core"                  % "2.9.0"
  val scalalib    = "com.github.ornicar"           %% "scalalib"                        % "9.0.1"
  val hasher      = "com.roundeights"              %% "hasher"                          % "1.3.1"
  val jodaTime    = "joda-time"                     % "joda-time"                       % "2.12.2"
  val chess       = "org.lichess"                  %% "scalachess"                      % "13.1.4"
  val compression = "org.lichess"                  %% "compression"                     % "1.8"
  val maxmind     = "com.maxmind.geoip2"            % "geoip2"                          % "3.0.2"
  val prismic     = "io.prismic"                   %% "scala-kit"                       % "1.2.19_lila-1"
  val caffeine    = "com.github.ben-manes.caffeine" % "caffeine"                        % "3.1.2" % "compile"
  val scaffeine   = "com.github.blemale"           %% "scaffeine"                       % "5.2.1" % "compile"
  val googleOAuth = "com.google.auth"               % "google-auth-library-oauth2-http" % "1.14.0"
  val galimatias  = "io.mola.galimatias"            % "galimatias"                      % "0.2.2-NF"
  val scalatags   = "com.lihaoyi"                  %% "scalatags"                       % "0.12.0"
  val lettuce     = "io.lettuce"                    % "lettuce-core"                    % "6.2.2.RELEASE"
  val nettyTransport =
    "io.netty" % s"netty-transport-native-$notifier" % "4.1.86.Final" classifier s"$os-$arch"
  val scalatest   = "org.scalatest"              %% "scalatest"    % "3.2.11" % Test
  val uaparser    = "org.uaparser"               %% "uap-scala"    % "0.14.0"
  val apacheText  = "org.apache.commons"          % "commons-text" % "1.10.0"
  val bloomFilter = "com.github.alexandrnikitin" %% "bloom-filter" % "0.13.1_lila-1"

  object specs2 {
    val version = "5.2.0"
    val core    = "org.specs2" %% "specs2-core" % version  % Test
    val cats    = "org.specs2" %% "specs2-cats" % "4.19.0" % Test
    val bundle  = Seq(core, cats)
  }

  object flexmark {
    val version = "0.64.0"
    val bundle =
      ("com.vladsch.flexmark" % "flexmark" % version) ::
        List("ext-tables", "ext-autolink", "ext-gfm-strikethrough").map { ext =>
          "com.vladsch.flexmark" % s"flexmark-$ext" % version
        }
  }

  object macwire {
    val version = "2.5.8"
    val macros  = "com.softwaremill.macwire" %% "macros"  % version % "provided"
    val util    = "com.softwaremill.macwire" %% "util"    % version % "provided"
    val tagging = "com.softwaremill.common"  %% "tagging" % "2.3.4"
    def bundle  = Seq(macros, util, tagging)
  }

  object reactivemongo {
    val version = "1.1.0-275c4ca-RC7-SNAPSHOT"

    val driver = "org.reactivemongo" %% "reactivemongo"               % version
    val stream = "org.reactivemongo" %% "reactivemongo-akkastream"    % "1.1.0-RC6"
    val shaded = "org.reactivemongo"  % "reactivemongo-shaded-native" % s"1.1.0-RC6-$os-x86-64"
    // val kamon  = "org.reactivemongo" %% "reactivemongo-kamon"         % "1.0.8"
    def bundle = Seq(driver, stream)
  }

  object play {
    val playVersion = "2.8.18-lila_3.7"
    val json        = "com.typesafe.play" %% "play-json"         % "2.10.0-RC7"
    val jsonJoda    = "com.typesafe.play" %% "play-json-joda"    % "2.10.0-RC7"
    val api         = "com.typesafe.play" %% "play"              % playVersion
    val server      = "com.typesafe.play" %% "play-server"       % playVersion
    val netty       = "com.typesafe.play" %% "play-netty-server" % playVersion
    val logback     = "com.typesafe.play" %% "play-logback"      % playVersion
  }

  object playWs {
    val version = "2.2.0-M2_lila-1"
    val ahc     = "com.typesafe.play" %% "play-ahc-ws-standalone"  % version
    val json    = "com.typesafe.play" %% "play-ws-standalone-json" % version
    val bundle  = Seq(ahc, json)
  }

  object kamon {
    val version    = "2.5.11"
    val core       = "io.kamon" %% "kamon-core"           % version
    val influxdb   = "io.kamon" %% "kamon-influxdb"       % version
    val metrics    = "io.kamon" %% "kamon-system-metrics" % version
    val prometheus = "io.kamon" %% "kamon-prometheus"     % version
  }
  object akka {
    val version    = "2.6.20"
    val actor      = "com.typesafe.akka" %% "akka-actor"       % version
    val actorTyped = "com.typesafe.akka" %% "akka-actor-typed" % version
    val akkaStream = "com.typesafe.akka" %% "akka-stream"      % version
    val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"       % version
    val testkit    = "com.typesafe.akka" %% "akka-testkit"     % version % Test
    def bundle     = List(actor, actorTyped, akkaStream, akkaSlf4j)
  }
}
