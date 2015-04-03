package ru.bugzmanov.prcheck.checks

import java.io.{File, StringWriter}
import java.util.Collections

import net.sourceforge.pmd._
import net.sourceforge.pmd.lang.java.JavaLanguageModule
import net.sourceforge.pmd.lang.{Language, LanguageFilenameFilter}
import net.sourceforge.pmd.renderers.Renderer
import net.sourceforge.pmd.util.FileUtil
import net.sourceforge.pmd.util.datasource.DataSource
import org.apache.commons.io.filefilter.AbstractFileFilter

import scala.collection.JavaConversions._



object PMDExecutor extends ViolationChecker {

  val description = "PMD"
  
  def execute(inputPath: String, filesWhitelist: Set[String] = Set()): Vector[ViolationIssue] = {
    val ruleSetFactory = new RuleSetFactory

    val languages: Set[Language] = Set(new JavaLanguageModule)
    val sourceFiles = getApplicableFiles(inputPath, languages, filesWhitelist)

    val renderer = new net.sourceforge.pmd.renderers.CSVRenderer()

    val writer: StringWriter = new StringWriter
    renderer.setWriter(writer)
    renderer.start()
    val ctx: RuleContext = new RuleContext
    val configuration = new PMDConfiguration()
    configuration.setThreads(3)
    configuration.setRuleSets("pmd_rules.xml")

//    configuration.setRuleSets("rulesets/java/basic.xml," +
//      "rulesets/java/braces.xml," +
//      "rulesets/java/codesize.xml," +
//
//      "rulesets/java/coupling.xml," +
//      "rulesets/java/design.xml," +
//      "rulesets/java/empty.xml," +
//      "rulesets/java/imports.xml," +
//      "rulesets/java/junit.xml," +
//      "rulesets/java/logging-java.xml," +
//      "rulesets/java/naming.xml," +
//      "rulesets/java/strictexception.xml," +
//      "rulesets/java/strings.xml," +
//      "rulesets/java/sunsecure.xml," +
//      "rulesets/java/unnecessary.xml," +
//      "rulesets/java/unusedcode.xml")
    //        "rulesets/java/comments.xml," +
      //        "rulesets/java/controversial.xml," +
      //        "rulesets/java/optimizations.xml," +

    PMD.processFiles(configuration, ruleSetFactory, sourceFiles, ctx, Collections.singletonList(renderer).asInstanceOf[java.util.List[Renderer]])
    renderer.end()
    renderer.flush()
    writer.getBuffer.toString.split("\n").toVector.tail.map(PMDIssue(_)).filterNot(f=> f.file.contains("src/test")
      && (f.rule == "LawOfDemeter" || f.rule == "LongVariable" || f.rule == "AbstractNaming"
      || f.rule == "JUnitTestsShouldIncludeAssert" || f.rule == "JUnitSpelling"))
  }

  def getApplicableFiles(inputPath: String, languages: Set[Language], filesWhitelist: Set[String]): java.util.List[DataSource] = {
    val fileSelector: LanguageFilenameFilter = new LanguageFilenameFilter(languages)
    FileUtil.collectFiles(inputPath, new AbstractFileFilter {

      fileSelector

      override def accept(file: File): Boolean = {
        fileSelector.accept(file.getParentFile, file.getName) && (filesWhitelist.isEmpty || filesWhitelist.contains(file.getCanonicalPath))
      }
    })
  }
}

object PMDIssue {

  def apply(str: String): ViolationIssue = {
    val tokens = str.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)").map(s => s.substring(1, s.length - 1))
    new ViolationIssue(tokens(1), tokens(2), tokens(3).toInt, tokens(4).toInt, tokens(5), tokens(6), tokens(7), tag = "pmd")
  }
}
