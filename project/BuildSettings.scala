import sbt._, Keys._
import play.Play.autoImport._

object BuildSettings {

  import Dependencies._

  def buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.lichess",
    scalaVersion := "2.11.4",
    resolvers ++= Dependencies.Resolvers.commons,
    parallelExecution in Test := false,
    scalacOptions := compilerOptions,
    // updateOptions := updateOptions.value.withCachedResolution(true),
    sources in doc in Compile := List())

  def defaultDeps = Seq(scalaz, scalalib, jodaTime, spray.util, ws)

  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  def project(name: String, deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty) =
    Project(
      name,
      file("modules/" + name),
      dependencies = deps,
      settings = Seq(
        version := "2.0",
        libraryDependencies := defaultDeps
      ) ++ buildSettings ++ srcMain
    )

  val compilerOptions = Seq("-deprecation", "-unchecked", "-feature", "-language:_")

  val srcMain = Seq(
    scalaSource in Compile <<= (sourceDirectory in Compile)(identity),
    scalaSource in Test <<= (sourceDirectory in Test)(identity)
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
