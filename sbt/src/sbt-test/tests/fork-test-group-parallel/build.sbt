val specs = "org.specs2" %% "specs2-core" % "4.3.4"
ThisBuild / scalaVersion := "2.12.11"

Global / concurrentRestrictions := Seq(Tags.limitAll(4))
libraryDependencies += specs % Test
inConfig(Test)(Seq(
  testGrouping := {
    val home = javaHome.value
    val strategy = outputStrategy.value
    val baseDir = baseDirectory.value
    val options = javaOptions.value
    val connect = connectInput.value
    val vars = envVars.value
    definedTests.value.map { test => new Tests.Group(test.name, Seq(test), Tests.SubProcess(
      ForkOptions(
        javaHome = home,
        outputStrategy = strategy,
        bootJars = Vector(),
        workingDirectory = Some(baseDir),
        runJVMOptions = options.toVector,
        connectInput = connect,
        envVars = vars
      )
    ))}
  },
  TaskKey[Unit]("test-failure") := test.failure.value
))
