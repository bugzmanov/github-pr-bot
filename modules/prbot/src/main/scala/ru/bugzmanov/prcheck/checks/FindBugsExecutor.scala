package ru.bugzmanov.prcheck.checks

import edu.umd.cs.findbugs.{FindBugs2, PrintingBugReporter}

object FindBugsExecutor extends ViolationChecker {
  override def execute(inputPath: String, fileFilter: Set[String] = Set()): Vector[ViolationIssue] = {

    val bugs: FindBugs2 = new FindBugs2()
    bugs.setBugReporter(new PrintingBugReporter())
    bugs.execute()
    Vector()
  }

  override def description: String = "findbugs"
}
