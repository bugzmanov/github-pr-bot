package ru.bugzmanov.prcheck.checks

import java.io.BufferedInputStream

import net.sourceforge.pmd._
import net.sourceforge.pmd.lang.Language
import net.sourceforge.pmd.lang.java.JavaLanguageModule
import net.sourceforge.pmd.util.datasource.DataSource

import scala.util.{Success, Try}

class JavaPmdExecutor private (config: PMDConfiguration) {
  val BlacklistedRules = Set("LawOfDemeter", "LongVariable", "AbstractNaming", "JUnitTestsShouldIncludeAssert", "JUnitSpelling")
  val ruleSetFactory = new RuleSetFactory
  val processor = new SourceCodeProcessor(config)
  val rules = RulesetsFactoryUtils.getRuleSets(config.getRuleSets, ruleSetFactory)

  def process(sources: DataSource*): Try[Iterable[ViolationIssue]] = {
    val reports = analyze(sources)
    val issues = reports.flatMap { report => JavaPmdExecutor.asList(report.iterator()) }.map(JavaPmdExecutor.asIssue)

    val filtered = issues
      .filterNot(isTestFile)
      .filterNot(issue => BlacklistedRules.contains(issue.rule))

    Success(filtered)
  }

  def isTestFile(vi: ViolationIssue) = vi.file.contains("src/test")

  private def analyze(sources: Iterable[DataSource]): Iterable[Report] = {
    val ctx = new RuleContext
    ctx.setLanguageVersion(null)

    sources.map { source =>
      val niceName = source.getNiceFileName(config.isReportShortNames, config.getInputPaths)
      val report = PMD.setupReport(rules, ctx, niceName)
      val bis = new BufferedInputStream(source.getInputStream)
      rules.start(ctx)
      processor.processSourceCode(bis, rules, ctx)
      rules.end(ctx)
      report
    }
  }
  
}

object JavaPmdExecutor {
  private[JavaPmdExecutor] final val langs: Set[Language] = Set(new JavaLanguageModule)

  private[JavaPmdExecutor] def asList[T](it: java.util.Iterator[T], acc: List[T] = Nil): List[T] = {
    if (it.hasNext) {
      asList(it, it.next::acc)
    } else {
      acc
    }
  }

  private[JavaPmdExecutor] def asIssue(violation: RuleViolation): ViolationIssue = ViolationIssue(
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
