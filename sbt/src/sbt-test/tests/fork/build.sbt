import Tests._
import Defaults._

val groupSize = 3
val groups = 3

val check = TaskKey[Unit]("check", "Check all files were created and remove them.")
val scalatest = "org.scalatest" %% "scalatest" % "3.0.5"
val scalaxml = "org.scala-lang.modules" %% "scala-xml" % "1.1.1"

def groupId(idx: Int) = "group_" + (idx + 1)
def groupPrefix(idx: Int) = groupId(idx) + "_file_"

ThisBuild / scalaVersion := "2.12.11"
ThisBuild / organization := "org.example"

lazy val root = (project in file("."))
  .settings(
    testGrouping in Test := {
      val tests = (definedTests in Test).value
      assert(tests.size == 3)
      for (idx <- 0 until groups) yield
        new Group(
          groupId(idx),
          tests,
          SubProcess(ForkOptions().withRunJVMOptions(Vector("-Dgroup.prefix=" + groupPrefix(idx))))
        )
    },
    check := {
      val files =
        for(i <- 0 until groups; j <- 1 to groupSize) yield
          file(groupPrefix(i) + j)
      val (exist, absent) = files.partition(_.exists)
      exist.foreach(_.delete())
      if(absent.nonEmpty)
        sys.error("Files were not created:\n\t" + absent.mkString("\n\t"))
    },
    concurrentRestrictions := Tags.limit(Tags.ForkedTestGroup, 2) :: Nil,
    libraryDependencies ++= List(
      scalaxml,
      scalatest % Test
    )
  )
