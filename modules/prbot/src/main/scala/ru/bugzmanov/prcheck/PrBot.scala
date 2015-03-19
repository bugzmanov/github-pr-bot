package ru.bugzmanov.prcheck

import java.io.File


class PrBot(token: String, botName: String) {

  val pullRequest = "https://github.com/(.*)/(.*)/pull/(.*)".r
  val pullRequestApi = "https://api.github.com/repos/(.*)/(.*)/pulls/(.*)".r

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
    
    val (commitComment, generalComment) = collectReviewComments(githubApi, prNumber, botName, Seq(PMDExecutor, CheckstyleExecutor))
    commitComment.foreach { c =>
      githubApi.publishComment(prNumber, c.body, c.commitId, c.path, c.lineNumber)
    }
    githubApi.publishPrComment(prNumber, generalComment)
  }

  def collectReviewComments(githubApi: GithubApi, prNumber: Int, botuser: String, checkers: Seq[ViolationChecker]): (Vector[Comment], String) = {
    val tmpDir: String = s"/tmp/github-${githubApi.repo}-${System.currentTimeMillis()}/"
    val prId: Int = prNumber.toInt
    val tmpDirFile: File = new File(tmpDir)

    val pullRequest = githubApi.describePR(prId)

    val diffContent: String = githubApi.downloadDiff(prId)

    val diffModel: Map[String, Patch] = DiffParser.
      parseMultiDiff(diffContent.split("\n").toList).map(
        k => (new File(tmpDir + k.revised).getCanonicalPath, k)
      ).toMap

    val git = githubApi.cloneTo(tmpDirFile, pullRequest.fromBranch)

    val violations = checkers.flatMap { check =>
      check.execute(tmpDir, diffModel.keySet)
        .filter(issue => diffModel.get(issue.file).exists(_.isWithinRevisedVersion(issue.line)))
    }

    val requiredPrComments = violations.map {f =>
      Comment(prId, f.file.substring(tmpDirFile.getCanonicalPath.size+1), f.line, pullRequest.fromCommit,  botuser, s"[${f.tag}] <${f.rule}> ${f.description}")
    }.toVector

    val newPrComments = {
      val oldBotComments = githubApi.getCommentsByUser(prNumber, botuser).map(_.uniqueKey).toSet
      requiredPrComments.filterNot(c => oldBotComments.contains(c.uniqueKey))
    }

    val generalComment = createGeneralComment(checkers, pullRequest, git, tmpDir, diffModel.keySet)
    
    git.clean()

    (newPrComments, generalComment )
  }

  
  private def createGeneralComment(checkers: Seq[ViolationChecker], prModel: PullRequest, git: GitApi, tmpDir: String, filter: Set[String]): String = {
    git.changeBranch(prModel.intoBranch)

    val before = checkers.map { check =>
      (check.description, check.execute(tmpDir, filter).toSet.size)
    }.toMap

    git.merge(prModel.fromBranch)

    val checksDiff = checkers.map { check =>
      (check.description, check.execute(tmpDir, filter).toSet.size - before(check.description))
    }.toMap

    val karma = if (checksDiff.values.sum < 0) s"@${prModel.author}++ for making code cleaner" else ""

    val generalComment = checksDiff.foldLeft("") { case (result, (check, diff)) =>
      result + s"$check violations diff: $diff\n"
    }
    generalComment + karma
  }
}
