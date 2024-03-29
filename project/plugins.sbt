libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
addSbtPlugin("com.alejandrohdezma" % "sbt-github-header" % "0.11.4")
addSbtPlugin("com.alejandrohdezma" % "sbt-github-mdoc" % "0.11.4")
addSbtPlugin("com.alejandrohdezma" % "sbt-github" % "0.11.4")
addSbtPlugin("com.alejandrohdezma" % "sbt-remove-test-from-pom" % "0.1.0")
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.11")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.9.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.3.1")
addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "1.2.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.0")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.7")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.9.0")
