/*
 * sbt
 * Copyright 2011 - 2018, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package sbt.internal

private[sbt] object Banner {
  def apply(version: String): Option[String] =
    version match {
      case "1.3.0" =>
        Some(s"""
                |Welcome to sbt $version.
                |Here are some highlights of this release:
                |  - Coursier: new default library management using https://get-coursier.io
                |  - Super shell: displays actively running tasks
                |  - Turbo mode: makes `test` and `run` faster in interactive sessions. Try it by running `set ThisBuild / turbo := true`.
                |See https://www.lightbend.com/blog/sbt-1.3.0-release for full release notes.
                |Hide the banner for this release by running `skipBanner`.
                |""".stripMargin.linesIterator.filter(_.nonEmpty).mkString("\n"))
      case _ => None
    }
}
