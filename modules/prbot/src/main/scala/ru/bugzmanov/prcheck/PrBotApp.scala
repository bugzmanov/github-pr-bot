package ru.bugzmanov.prcheck

import java.io.File


object PrBotApp {

  val pullRequest = "https://github.com/(.*)/(.*)/pull/(.*)".r
  val pullRequestApi = "https://api.github.com/repos/(.*)/(.*)/pulls/(.*)".r

  def main(args: Array[String]) {
  }

  def doCodeReviewApiCall(url: String, token: String) = {
    val pullRequestApi(account, repo, prNumber) = url
    doCodeReview(account, repo, prNumber, token)
  }

  def doCodeReview(url: String, token: String): Unit = {
    val pullRequest(account, repo, prNumber) = url
    doCodeReview(account, repo, prNumber, token)
  }

  def doCodeReview(account: String, repo: String, prNumber: String, token: String): Unit = {
    val tmpDir: String = s"/tmp/github-$repo/"
    val prId: Int = prNumber.toInt
    val tmpDirFile: File = new File(tmpDir)

    val githubApi: GithubApi = GithubApi.tokenBased(account, repo, token)
    val prModel = githubApi.describePR(prId)

    val diffContent: String = githubApi.downloadDiff(prId)

    val diffModel: Map[String, Patch] = DiffParser.
      parseMultiDiff(diffContent.split("\n").toList).map(
        k => (new File(tmpDir + k.revised).getCanonicalPath, k)
      ).toMap


    val git: GitApi = githubApi.cloneTo(tmpDirFile, prModel.fromBranch)

    
//    val user: Vector[Comment] = githubApi.getCommentsByUser(prNumber.toInt, "iasbot")
//    githubApi.cleanPrComments(user)
    val pmdIssues = PMDExecutor.execute(tmpDir, diffModel.keySet)
      .filter(issue => diffModel.get(issue.file).exists(_.isWithinRevisedVersion(issue.line)))

    val checkStyleIssues = CheckstyleExecutor.execute(tmpDir, diffModel.keySet)
      .filter(issue => diffModel.get(issue.file).exists(_.isWithinRevisedVersion(issue.line)))

    (pmdIssues  ++ checkStyleIssues).foreach{f =>
      githubApi.publishComment(prId, s"[${f.tag}] ${f.description}", prModel.fromCommit, f.file.substring(tmpDirFile.getCanonicalPath.size+1), f.line)}

    git.changeBranch(prModel.intoBranch)
    val pmdBefore = PMDExecutor.execute(tmpDir, diffModel.keySet).toSet.size
    val checkstyleBefore =  CheckstyleExecutor.execute(tmpDir, diffModel.keySet).toSet.size

    git.merge(prModel.fromBranch)
    val pmdAfter = PMDExecutor.execute(tmpDir, diffModel.keySet).toSet.size
    val checkstyleAfter = CheckstyleExecutor.execute(tmpDir, diffModel.keySet).toSet.size

    val karma = if (pmdAfter - pmdBefore > 0 || checkstyleAfter - checkstyleBefore > 0) s"@${prModel.author}++ for making code cleaner" else ""

    githubApi.publishPrComment(prId,s"""
       | PMD Viloations after PR: ${pmdAfter - pmdBefore}\n
       | Checkstyle Viloations after PR: ${checkstyleAfter - checkstyleBefore}\n
       | \n
       | $karma
     """.stripMargin)

    git.clean()
  }
}
