val scalatest = "org.scalatest" %% "scalatest" % "3.0.5"
ThisBuild / scalaVersion := "2.12.11"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies += scalatest % Test,
    Test / parallelExecution := false
  )
