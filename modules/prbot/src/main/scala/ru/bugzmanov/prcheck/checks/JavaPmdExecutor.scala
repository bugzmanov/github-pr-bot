package ru.bugzmanov.prcheck.checks

import java.io.BufferedInputStream
import javax.annotation.concurrent.NotThreadSafe

import net.sourceforge.pmd._
import net.sourceforge.pmd.lang.Language
import net.sourceforge.pmd.lang.java.JavaLanguageModule
import net.sourceforge.pmd.util.datasource.DataSource
import scala.collection.JavaConverters._

import scala.util.{Failure, Success, Try}

@NotThreadSafe
class JavaPmdExecutor private (config: PMDConfiguration) {
  val BlacklistedRules = Set("LawOfDemeter", "LongVariable", "AbstractNaming", "JUnitTestsShouldIncludeAssert", "JUnitSpelling")
  val ruleSetFactory = new RuleSetFactory
  val processor = new SourceCodeProcessor(config)
  val rules = RulesetsFactoryUtils.getRuleSets(config.getRuleSets, ruleSetFactory)
  val ctx = new RuleContext

  def analyze(source: DataSource): Try[Vector[ViolationIssue]] = {
    val report: Report = process(source)

    if (report.hasConfigErrors) {
      val errors = report.configErrors().asScala.mkString("{", ", ", "}")
      Failure(new IllegalArgumentException(s"Configuration errors: $errors"))
    } else if(report.hasErrors) {
      Failure(???)
    } else {
      val scalified = report.iterator().asScala
      val converted = scalified.map(JavaPmdExecutor.asIssue)
      val filtered = converted
        .filterNot(isTestFile)
        .filterNot(issue => BlacklistedRules.contains(issue.rule))

      Success(filtered.toVector)
    }
  }

  private def isTestFile(vi: ViolationIssue) = vi.file.contains("src/test")

  private def process(source: DataSource): Report = {
    ctx.setLanguageVersion(null) // in original code, version is said to be unknown and should be auto derived

    val niceName = source.getNiceFileName(config.isReportShortNames, config.getInputPaths)
    val report = PMD.setupReport(rules, ctx, niceName)
    val bis = new BufferedInputStream(source.getInputStream)
    rules.start(ctx)
    processor.processSourceCode(bis, rules, ctx)
    rules.end(ctx)
    report
  }
  
}

object JavaPmdExecutor {
  private final val langs: Set[Language] = Set(new JavaLanguageModule)

  private def asIssue(violation: RuleViolation) = ViolationIssue(
    classPackage = violation.getPackageName,
    file = violation.getFilename,
    priority = violation.getRule.getPriority.getPriority,
    line = violation.getBeginLine,
    description = violation.getDescription,
    ruleSet = violation.getRule.getRuleSetName,
    rule = violation.getRule.getName,
    tag = "pmd"
  )

  def fromRulesFile(classpath: String): JavaPmdExecutor = {
    val config = new PMDConfiguration
    config.setRuleSets(classpath)
    new JavaPmdExecutor(config)
  }
}
