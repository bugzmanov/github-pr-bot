package ru.bugzmanov.prcheck.checks

trait ViolationChecker {
  def execute(inputPath: String, fileFilter: Set[String] = Set()): Vector[ViolationIssue]
  def description: String
}


case class ViolationIssue(
  classPackage: String,
  file: String,
  priority: Int,
  line: Int,
  description: String,
  ruleSet: String,
  rule: String,
  tag: String
)
