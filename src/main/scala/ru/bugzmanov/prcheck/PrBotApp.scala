package ru.bugzmanov.prcheck

import java.io.File


object PrBotApp {

  val pullRequest = "https://github.com/(.*)/(.*)/pull/(.*)".r

  def main(args: Array[String]) {
    doCodeReview(args(0), args(1))
  }

  def doCodeReview(url: String, token: String): Unit = {
    val pullRequest(account, repo, prNumber) = url
    val tmpDir: String = s"/tmp/github-$repo"
    val prId: Int = prNumber.toInt

    val githubApi: GithubApi = GithubApi.tokenBased(account, repo, token)
    val prModel = githubApi.describePR(prId)

    val diffContent: String = githubApi.downloadDiff(prId)

    val diffModel: Map[String, Patch] =
      DiffParser.parseMultiDiff(diffContent.split("\n").toList).map(k => (tmpDir + k.revised, k)).toMap

    val git: GitApi = githubApi.cloneTo(new File(tmpDir), prModel.fromBranch)

    val execute = PMDExecutor.execute(tmpDir)
    val filtered = execute.filter(issue => diffModel.get(issue.file).exists(_.isWithinRevisedVersion(issue.line)))

//    filtered.foreach(f =>
//      println(s"${f.ruleSet}:${f.rule} ${f.description} -   ${f.line}:${f.file.substring(tmpDir.size)}"))

    filtered.foreach(f =>
      githubApi.publishComment(prId, f.description, prModel.fromCommit, f.file.substring(tmpDir.size), f.line))


    git.changeBranch(prModel.intoBranch)
    val before: Set[ViolationIssue] = PMDExecutor.execute(tmpDir).toSet

    git.merge(prModel.fromBranch)
    val after = PMDExecutor.execute(tmpDir).toSet

    githubApi.publishPrComment(prId, s"PMD Violations before PR: ${before.size} \n" +
      s"PMD Violations after PR: ${after.size}")

    git.clean()
//    println(s"PMD Violations before PR: ${before.size} \n" +
//      s"PMD Violations after PR: ${after.size}")
  }
}
