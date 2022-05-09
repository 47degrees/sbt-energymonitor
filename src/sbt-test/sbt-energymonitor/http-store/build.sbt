version := "0.1"
scalaVersion := "2.13.8"

ThisBuild / energyMonitorDisableSampling := false
ThisBuild / energyMonitorPersistenceServerUrl := "http://192.168.8.137:8080"

lazy val httpTest = (project in file("."))
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test
  )
