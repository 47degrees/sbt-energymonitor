version := "0.1"
scalaVersion := "2.13.8"

lazy val httpTest = (project in file("."))
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test
  )
