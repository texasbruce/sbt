ThisBuild / scalaVersion := "2.13.0"
ivyPaths := IvyPaths(baseDirectory.value, Some(target.value / "ivy-cache"))

// don't blow up when credential file doesn't exist
// https://github.com/sbt/sbt/issues/4882
credentials += Credentials(baseDirectory.value / "nonexistent")
