package ru.bugzmanov.prcheck

import edu.umd.cs.findbugs.{PrintingBugReporter, FindBugs2, BugCollectionBugReporter, FindBugs}


object FindBugsExecutor extends ViolationChecker {
  override def execute(inputPath: String, fileFilter: Set[String] = Set()): Vector[ViolationIssue] = {

    val bugs: FindBugs2 = new FindBugs2()
    bugs.setBugReporter(new PrintingBugReporter())
    bugs.execute()
    Vector()
  }

  override def description: String = "findbugs"
}
