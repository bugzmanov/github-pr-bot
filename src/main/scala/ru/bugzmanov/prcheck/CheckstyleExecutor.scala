package ru.bugzmanov.prcheck

import java.util
import java.util.Collections

import com.puppycrawl.tools.checkstyle.{PropertiesExpander, ConfigurationLoader, Checker}
import com.puppycrawl.tools.checkstyle.api.{SeverityLevel, AuditEvent, AuditListener}
import java.io.File

import scala.collection.mutable

object CheckstyleExecutor extends ViolationChecker {

  override def execute(inputPath: String, fileFilter: Set[String] = Set()): Vector[ViolationIssue] = {
    val checker = new Checker
    val listFiles: Array[File] = recursiveListFiles(new File(inputPath))
      .filter( t=> fileFilter.isEmpty || fileFilter.contains(t.getCanonicalPath))

    val files = util.Arrays.asList(listFiles: _*)
    val checks = this.getClass.getResource("/google_checks.xml").getFile
    val config = ConfigurationLoader.loadConfiguration(checks, new PropertiesExpander(System.getProperties))
    val moduleClassLoader: ClassLoader = classOf[Checker].getClassLoader
    checker.setModuleClassLoader(moduleClassLoader)
    val listener: CheckstylAuditListener = new CheckstylAuditListener
    checker.configure(config)


    checker.addListener(listener)
    checker.process(files)
    listener.getResult
  }

  override def description: String = "style"

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these.filterNot(_.isDirectory) ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

}

class CheckstylAuditListener extends AuditListener {
  private val violations = Vector.newBuilder[ViolationIssue]

  override def auditStarted(evt: AuditEvent): Unit = {}

  override def addError(evt: AuditEvent): Unit = {
    violations += new ViolationIssue(
      classPackage = "undefined",
      file = new File(evt.getFileName).getCanonicalPath,
      priority = evt.getSeverityLevel match {
        case SeverityLevel.IGNORE => 5
        case  SeverityLevel.ERROR => 1
        case SeverityLevel.WARNING => 3
        case SeverityLevel.INFO => 4
      },
      line = evt.getLine,
      description = evt.getMessage,
      ruleSet = evt.getModuleId,
      rule = evt.getSourceName,
      tag = CheckstyleExecutor.description
    )
  }

  override def fileStarted(evt: AuditEvent): Unit = {}

  override def fileFinished(evt: AuditEvent): Unit = {}

  override def addException(evt: AuditEvent, throwable: Throwable): Unit = {}

  override def auditFinished(evt: AuditEvent): Unit = {}

  def getResult = violations.result()
}