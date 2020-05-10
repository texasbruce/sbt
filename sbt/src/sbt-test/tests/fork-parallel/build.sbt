import Tests._
import Defaults._

ThisBuild / scalaVersion := "2.12.11"
val check = taskKey[Unit]("Check that tests are executed in parallel")

lazy val root = (project in file("."))
  .settings(
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    fork in Test := true,
    check := {
      val nbProc = java.lang.Runtime.getRuntime().availableProcessors()
      val log = streams.value.log
      if( nbProc < 4 ) {
        log.warn("With fewer than 4 processors this test is meaningless")
        // mimic behavior expected by scripted
        if (!testForkedParallel.value) sys.error("Exiting with error (note: test not performed)")
      } else {
        // we've got at least 4 processors, we'll check the upper end but also 3 and 4 as the upper might not
        // be reached if the system is under heavy load.
        if( ! (file("max-concurrent-tests_3").exists() || file("max-concurrent-tests_4").exists() ||
               file("max-concurrent-tests_" + (nbProc -1)).exists() || file("max-concurrent-tests_" + nbProc).exists())) {
          sys.error("Forked tests were not executed in parallel!")
        }
      }
    }
  )
