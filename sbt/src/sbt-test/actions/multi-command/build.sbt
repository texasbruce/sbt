import Build._

organization := "sbt"

name := "scripted-multi-command-parser"

setStringValue := setStringValueImpl.evaluated

checkStringValue := checkStringValueImpl.evaluated

taskThatFails := {
  throw new IllegalArgumentException("")
  ()
}

checkInput := checkInputImpl.evaluated

val dynamicTask = taskKey[Unit]("dynamic input task")

dynamicTask := { println("not yet et") }
