/*
 * sbt
 * Copyright 2011 - 2018, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package sbt
package internal
package server

import java.io.File
import sbt.internal.inc.ManagedLoggedReporter
import sbt.internal.util.ManagedLogger
import xsbti.{ FileConverter, Problem, Position => XPosition, Severity }
import xsbti.compile.CompileAnalysis
import sbt.internal.langserver.{
  PublishDiagnosticsParams,
  Position,
  Diagnostic,
  Range,
  DiagnosticSeverity
}
import sbt.internal.inc.JavaInterfaceUtil._
import scala.collection.mutable
import scala.collection.JavaConverters._

/**
 * Defines a compiler reporter that uses event logging provided by a `ManagedLogger`.
 *
 * @param maximumErrors The maximum errors.
 * @param logger The event managed logger.
 * @param sourcePositionMapper The position mapper.
 */
class LanguageServerReporter(
    maximumErrors: Int,
    logger: ManagedLogger,
    sourcePositionMapper: XPosition => XPosition = identity[XPosition],
    converter: FileConverter
) extends ManagedLoggedReporter(maximumErrors, logger, sourcePositionMapper) {
  lazy val exchange = StandardMain.exchange

  private[sbt] lazy val problemsByFile = new mutable.HashMap[File, mutable.ListBuffer[Problem]]

  override def reset(): Unit = {
    super.reset()
    problemsByFile.clear()
  }

  override def log(problem: Problem): Unit = {
    val pos = problem.position
    pos.sourceFile.toOption foreach { sourceFile: File =>
      problemsByFile.get(sourceFile) match {
        case Some(xs: mutable.ListBuffer[Problem]) => problemsByFile(sourceFile) = xs :+ problem
        case _                                     => problemsByFile(sourceFile) = mutable.ListBuffer(problem)
      }
    }
    super.log(problem)
  }

  override def logError(problem: Problem): Unit = {
    aggregateProblems(problem)

    // console channel can keep using the xsbi.Problem
    super.logError(problem)
  }

  override def logWarning(problem: Problem): Unit = {
    aggregateProblems(problem)

    // console channel can keep using the xsbi.Problem
    super.logWarning(problem)
  }

  override def logInfo(problem: Problem): Unit = {
    aggregateProblems(problem)

    // console channel can keep using the xsbi.Problem
    super.logInfo(problem)
  }

  private[sbt] def resetPrevious(analysis: CompileAnalysis): Unit = {
    import sbt.internal.langserver.codec.JsonProtocol._
    val files = analysis.readSourceInfos.getAllSourceInfos.keySet.asScala
    files foreach { f =>
      val p = converter.toPath(f)
      val params = PublishDiagnosticsParams(p.toUri.toString, Vector())
      exchange.notifyEvent("textDocument/publishDiagnostics", params)
    }
  }

  private[sbt] def aggregateProblems(problem: Problem): Unit = {
    import sbt.internal.langserver.codec.JsonProtocol._
    val pos = problem.position
    pos.sourceFile.toOption foreach { sourceFile: File =>
      problemsByFile.get(sourceFile) match {
        case Some(xs: mutable.ListBuffer[Problem]) =>
          val ds = toDiagnostics(xs)
          val params = PublishDiagnosticsParams(sbt.io.IO.toURI(sourceFile).toString, ds)
          exchange.notifyEvent("textDocument/publishDiagnostics", params)
        case _ =>
      }
    }
  }

  private[sbt] def toDiagnostics(ps: Seq[Problem]): Vector[Diagnostic] = {
    for {
      problem <- ps.toVector
      pos = problem.position
      line0 <- pos.line.toOption.toVector
      pointer0 <- pos.pointer.toOption.toVector
    } yield {
      val line = line0.toLong - 1L
      val pointer = pointer0.toLong
      val r = (
        pos.startLine.toOption,
        pos.startColumn.toOption,
        pos.endLine.toOption,
        pos.endColumn.toOption
      ) match {
        case (Some(sl), Some(sc), Some(el), Some(ec)) =>
          Range(Position(sl.toLong - 1, sc.toLong), Position(el.toLong - 1, ec.toLong))
        case _ =>
          Range(Position(line, pointer), Position(line, pointer + 1))
      }
      Diagnostic(
        r,
        Option(toDiagnosticSeverity(problem.severity)),
        None,
        Option("sbt"),
        problem.message
      )
    }
  }

  private[sbt] def toDiagnosticSeverity(severity: Severity): Long = severity match {
    case Severity.Info  => DiagnosticSeverity.Information
    case Severity.Warn  => DiagnosticSeverity.Warning
    case Severity.Error => DiagnosticSeverity.Error
  }
}
