resolvers += Resolver.url(
  "lila-maven-sbt",
  url("https://raw.githubusercontent.com/lichess-org/lila-maven/master")
)(Resolver.ivyStylePatterns)
addSbtPlugin("com.typesafe.play" % "sbt-plugin"   % "2.8.16-lila_1.17")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"    % "1.5.4")
