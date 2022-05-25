name := """sbt-energymonitor"""
organization := "com.47deg"

ThisBuild / githubOrganization := "47degrees"

ThisBuild / Compile / run / fork := true

lazy val Version = new {
  val caseInsensitive = "1.2.0"
  val cats = "2.7.0"
  val catsEffect = "3.3.12"
  val catsScalacheck = "0.3.1"
  val circe = "0.14.1"
  val decline = "2.2.0"
  val disciplineMunit = "1.0.9"
  val flyway = "8.5.11"
  val fs2 = "3.2.5"
  val github4s = "0.31.0"
  val http4s = "0.23.12"
  val ip4s = "3.1.2"
  val log4cats = "2.3.0"
  val logback = "1.2.11"
  val munit = "0.7.29"
  val munitCatsEffect = "1.0.7"
  val natchez = "0.1.6"
  val postgres = "42.3.5"
  val scalacheckEffect = "1.0.4"
  val skunk = "0.3.1"
  val sourcepos = "1.0.1"
  val squants = "1.8.3"
  val testContainersScala = "0.40.7"
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
    "energyMonitorPersistenceCoreJVM/test",
    "energyMonitorPersistenceAppJVM/test",
    "energyMonitorPersistenceAppJVM/docker"
    // the JS app implementation is untested for now, since there are some
    // linking errors and it's not critical to the current scope of work
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
        "org.typelevel" %%% "scalacheck-effect-munit" % Version.scalacheckEffect % Test,
        "org.typelevel" %%% "cats-core" % Version.cats,
        "org.typelevel" %%% "cats-effect" % Version.catsEffect,
        "org.typelevel" %%% "cats-effect-kernel" % Version.catsEffect,
        "org.typelevel" %%% "cats-kernel" % Version.cats,
        "org.typelevel" %%% "discipline-munit" % Version.disciplineMunit % Test,
        "org.typelevel" %%% "munit-cats-effect-3" % Version.munitCatsEffect % Test,
        "org.typelevel" %%% "squants" % Version.squants,
        "org.http4s" %%% "http4s-circe" % Version.http4s,
        "org.http4s" %%% "http4s-core" % Version.http4s,
        "org.http4s" %%% "http4s-dsl" % Version.http4s
      )
    )

lazy val appImageName = "energy-monitor-persistence-app"

lazy val energyMonitorPersistenceApp =
  (crossProject(JSPlatform, JVMPlatform) in file(
    "energy-monitor-persistence-app"
  ))
    .dependsOn(energyMonitorPersistenceCore)
    .settings(
      libraryDependencies ++= Seq(
        "co.fs2" %%% "fs2-core" % Version.fs2,
        "co.fs2" %%% "fs2-io" % Version.fs2,
        "com.monovore" %%% "decline" % Version.decline,
        "org.http4s" %%% "http4s-core" % Version.http4s,
        "org.http4s" %%% "http4s-ember-server" % Version.http4s,
        "org.http4s" %%% "http4s-server" % Version.http4s,
        "org.scalameta" %%% "munit" % Version.munit % Test,
        "org.tpolecat" %%% "natchez-core" % Version.natchez,
        "org.tpolecat" %%% "skunk-core" % Version.skunk,
        "org.tpolecat" %%% "sourcepos" % Version.sourcepos,
        "org.typelevel" %% "case-insensitive" % Version.caseInsensitive,
        "org.typelevel" %% "cats-core" % Version.cats,
        "org.typelevel" %% "cats-effect-kernel" % Version.catsEffect,
        "org.typelevel" %% "cats-effect-std" % Version.catsEffect,
        "org.typelevel" %% "cats-effect" % Version.catsEffect,
        "org.typelevel" %% "cats-kernel" % Version.cats,
        "org.typelevel" %%% "munit-cats-effect-3" % Version.munitCatsEffect % Test,
        "org.typelevel" %%% "scalacheck-effect-munit" % Version.scalacheckEffect % Test,
        "org.typelevel" %%% "squants" % Version.squants
      )
    )
    .settings(
      appSettings: _*
    )
    .jsSettings(
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % Version.logback % Runtime,
        "com.dimafeng" %% "testcontainers-scala-postgresql" % Version.testContainersScala % Test,
        "org.flywaydb" % "flyway-core" % Version.flyway % Test,
        "org.postgresql" % "postgresql" % Version.postgres % Test
      ),
      docker / dockerfile := {
        val appDir: File = stage.value
        val targetDir = "/app"

        new Dockerfile {
          from("openjdk:8-jre")
          entryPoint(s"$targetDir/bin/${executableScriptName.value}")
          copy(appDir, targetDir, chown = "daemon:daemon")
          expose(8080)
        }
      },
      docker / imageNames := Seq(
        ImageName(s"${organization.value}/$appImageName:latest")
      )
    )
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
