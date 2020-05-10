/*
 * sbt
 * Copyright 2011 - 2018, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package sbt.internal.util

import java.io.{ BufferedWriter, PrintStream, PrintWriter }

sealed trait ConsoleOut {
  val lockObject: AnyRef
  def print(s: String): Unit
  def println(s: String): Unit
  def println(): Unit
  def flush(): Unit
}

object ConsoleOut {
  def systemOut: ConsoleOut = printStreamOut(System.out)

  def overwriteContaining(s: String): (String, String) => Boolean =
    (cur, prev) => cur.contains(s) && prev.contains(s)

  /** Move to beginning of previous line and clear the line. */
  private[this] final val OverwriteLine = "\u001B[A\r\u001B[2K"

  /**
   * ConsoleOut instance that is backed by System.out.  It overwrites the previously printed line
   * if the function `f(lineToWrite, previousLine)` returns true.
   *
   * The ConsoleOut returned by this method assumes that the only newlines are from println calls
   * and not in the String arguments.
   */
  def systemOutOverwrite(f: (String, String) => Boolean): ConsoleOut = new ConsoleOut {
    val lockObject = System.out
    private[this] var last: Option[String] = None
    private[this] var current = new java.lang.StringBuffer
    def print(s: String): Unit = synchronized { current.append(s); () }
    def println(s: String): Unit = synchronized { current.append(s); println() }
    def println(): Unit = synchronized {
      val s = current.toString
      if (ConsoleAppender.formatEnabledInEnv && last.exists(lmsg => f(s, lmsg)))
        lockObject.print(OverwriteLine)
      lockObject.println(s)
      last = Some(s)
      current.setLength(0)
    }
    def flush(): Unit = synchronized {
      val s = current.toString
      if (ConsoleAppender.formatEnabledInEnv && last.exists(lmsg => f(s, lmsg)))
        lockObject.print(OverwriteLine)
      lockObject.print(s)
      last = Some(s)
      current.setLength(0)
    }
  }

  def printStreamOut(out: PrintStream): ConsoleOut = new ConsoleOut {
    val lockObject = out
    def print(s: String) = out.print(s)
    def println(s: String) = out.println(s)
    def println() = out.println()
    def flush() = out.flush()
  }
  def printWriterOut(out: PrintWriter): ConsoleOut = new ConsoleOut {
    val lockObject = out
    def print(s: String) = out.print(s)
    def println(s: String) = { out.println(s); flush() }
    def println() = { out.println(); flush() }
    def flush() = { out.flush() }
  }
  def bufferedWriterOut(out: BufferedWriter): ConsoleOut = new ConsoleOut {
    val lockObject = out
    def print(s: String) = out.write(s)
    def println(s: String) = { out.write(s); println() }
    def println() = { out.newLine(); flush() }
    def flush() = { out.flush() }
  }
}
