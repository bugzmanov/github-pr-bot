package ru.bugzmanov.prcheck

import ru.bugzmanov.prcheck.checks.{PMDExecutor, CheckstyleExecutor}


class PullRequestBot(token: String, botName: String) {

  val pullRequest = "https://github.com/(.*)/(.*)/pull/(.*)".r
  val pullRequestApi = "https://api.github.com/repos/(.*)/(.*)/pulls/(.*)".r

  val reviewer = new CodeReviewer(botName, Seq(PMDExecutor, CheckstyleExecutor))

  def updateDescriprtion(url: String, description: String) = {
    val pullRequestApi(account, repo, prNumber) = url
    val githubApi: GithubApi = GithubApi.tokenBased(account, repo, token)
    githubApi.publishPrComment(prNumber.toInt, description)
  }

  def removeComments(url: String) = {
    val pullRequestApi(account, repo, prNumber) = url
    val githubApi: GithubApi = GithubApi.tokenBased(account, repo, token)
    val botComments = githubApi.getCommentsByUser(prNumber.toInt, botName)
    githubApi.cleanCommitComments(botComments)
  }

  def publishComment(url: String, message: String) = {
    val pullRequestApi(account, repo, prNumber) = url
    val githubApi: GithubApi = GithubApi.tokenBased(account, repo, token)
    githubApi.publishPrComment(prNumber.toInt, message)
  }

  def runReviewOnApiCall(url: String) = {
    val pullRequestApi(account, repo, prNumber) = url
    val githubApi: GithubApi = GithubApi.tokenBased(account, repo, token)
    makeReview(prNumber.toInt, githubApi)
  }

  def runReview(url: String): Unit = {
    val pullRequest(account, repo, prNumber) = url
    val githubApi: GithubApi = GithubApi.tokenBased(account, repo, token)

    makeReview(prNumber.toInt, githubApi)
  }

  def makeReview(prNumber: Int, githubApi: GithubApi): Unit = {
    
    val (commitComment, generalComment) = reviewer.collectReviewComments(githubApi, prNumber)
    commitComment.foreach { c =>
      githubApi.publishCommentFake(prNumber, c.body, c.commitId, c.path, c.lineNumber)
    }
    githubApi.publishPrCommentFake(prNumber, generalComment)
  }
}
