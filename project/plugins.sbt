libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.2")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.5")
addSbtPlugin("com.alejandrohdezma" % "sbt-github" % "0.11.2")
addSbtPlugin("com.alejandrohdezma" % "sbt-github-header" % "0.11.2")
addSbtPlugin("com.alejandrohdezma" % "sbt-github-mdoc" % "0.11.2")
addSbtPlugin("com.alejandrohdezma" % "sbt-remove-test-from-pom" % "0.1.0")
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.2.0")
