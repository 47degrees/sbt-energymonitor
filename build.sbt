name := """sbt-energymonitor"""
organization := "com.47deg"

ThisBuild / githubOrganization := "47degrees"

lazy val Version = new {
  val cats = "2.7.0"
  val catsEffect = "3.3.11"
  val catsScalacheck = "0.3.1"
  val circe = "0.14.1"
  val disciplineMunit = "1.0.9"
  val github4s = "0.31.0"
  val http4s = "0.23.11"
  val munit = "0.7.29"
  val squants = "1.8.3"
  val weaver = "0.7.11"
}

addCommandAlias(
  "ci-test",
  List(
    "scalafmtCheckAll",
    "scalafmtSbtCheck",
    "energyMonitorPlugin/test",
    "energyMonitorPlugin/publishLocal",
    "energyMonitorPlugin/scripted",
    "energyMonitorPersistenceCoreJS/test",
    "energyMonitorPersistenceCoreJVM/test"
  ).mkString(";")
)
addCommandAlias("ci-publish", "github; ci-release")
addCommandAlias("ci-docs", ";github; mdoc; headerCreateAll")

inThisBuild(
  List(
    organization := "com.47deg",
    homepage := Some(url("https://github.com/47degrees/sbt-energymonitor")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    )
  )
)

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))
  .settings(publish / skip := true)

lazy val energyMonitorPlugin =
  (project in file("energy-monitor-plugin"))
    .enablePlugins(ScriptedPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "io.chrisdavenport" %% "cats-scalacheck" % Version.catsScalacheck % Test,
        "com.disneystreaming" %% "weaver-cats" % Version.weaver % Test,
        "com.disneystreaming" %% "weaver-scalacheck" % Version.weaver % Test,
        "com.47deg" %% "github4s" % Version.github4s,
        "io.circe" %% "circe-core" % Version.circe,
        "io.circe" %% "circe-parser" % Version.circe,
        "org.http4s" %% "http4s-blaze-client" % Version.http4s,
        "org.scala-sbt" %% "collections" % sbtVersion.value,
        "org.scala-sbt" %% "core-macros" % sbtVersion.value,
        "org.scala-sbt" %% "main" % sbtVersion.value,
        "org.scala-sbt" %% "main-settings" % sbtVersion.value,
        "org.scala-sbt" % "sbt" % sbtVersion.value,
        "org.scala-sbt" %% "task-system" % sbtVersion.value,
        "org.scala-sbt" %% "util-position" % sbtVersion.value,
        "org.typelevel" %% "cats-core" % Version.cats,
        "org.typelevel" %% "cats-effect" % Version.catsEffect,
        "org.typelevel" %% "cats-effect-kernel" % Version.catsEffect,
        "org.typelevel" %% "cats-kernel" % Version.cats
      ),
      testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
      sbtPlugin := true,
      // set up 'scripted; sbt plugin for testing sbt plugins
      scriptedLaunchOpts ++=
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    )

lazy val appSettings = Seq(
  scalaVersion := "2.13.8",
  crossScalaVersions := Seq(
    "2.13.8",
    "3.1.2"
  )
)

lazy val energyMonitorPersistenceCore =
  (crossProject(JSPlatform, JVMPlatform) in file(
    "energy-monitor-persistence-core"
  ))
    .settings(appSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-core" % Version.circe,
        "io.circe" %%% "circe-testing" % Version.circe % Test,
        "org.scalameta" %%% "munit" % Version.munit % Test,
        "org.typelevel" %%% "cats-core" % Version.cats,
        "org.typelevel" %%% "cats-effect" % Version.catsEffect,
        "org.typelevel" %%% "cats-effect-kernel" % Version.catsEffect,
        "org.typelevel" %%% "cats-kernel" % Version.cats,
        "org.typelevel" %%% "discipline-munit" % Version.disciplineMunit % Test,
        "org.typelevel" %%% "squants" % Version.squants,
        "org.http4s" %%% "http4s-circe" % Version.http4s,
        "org.http4s" %%% "http4s-core" % Version.http4s,
        "org.http4s" %%% "http4s-dsl" % Version.http4s
      )
    )

lazy val energyMonitorPersistenceApp =
  (crossProject(JSPlatform, JVMPlatform) in file(
    "energy-monitor-persistence-app"
  ))
    .dependsOn(energyMonitorPersistenceCore)
    .settings(
      libraryDependencies ++= Seq(
        "org.tpolecat" %%% "skunk-core" % "0.2.3"
      )
    )
    .settings(
      appSettings: _*
    )
