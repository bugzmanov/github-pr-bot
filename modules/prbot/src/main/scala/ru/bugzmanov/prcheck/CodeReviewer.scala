package ru.bugzmanov.prcheck

import java.io.File

import ru.bugzmanov.prcheck.checks.{CheckstyleExecutor, ViolationChecker}

class CodeReviewer(botuser: String, checkers: Seq[ViolationChecker]) {

  def collectReviewComments(githubApi: GithubApi, prNumber: Int): (Vector[Comment], String) = {
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

    val requiredPrComments = violations.groupBy(_.file).flatMap{ case (file, viols) =>
      val (tabChecks, other) = viols.partition(_.rule == CheckstyleExecutor.tabCheck)
      other ++ Seq(tabChecks.head.copy(description = s"Achtung! ${tabChecks.size} lines with tab characters in this PR in this file"))
    }.map {f =>
      val relativePath = f.file.substring(tmpDirFile.getCanonicalPath.size + 1)
      Comment(prId, relativePath, f.line, pullRequest.fromCommit,  botuser, s"[${f.tag}] <${f.rule}> ${f.description}")
    }.toVector

    val newPrComments = {
      val oldBotComments = githubApi.getCommentsByUser(prNumber, botuser).map(_.uniqueKey).toSet
      requiredPrComments.filterNot(c => oldBotComments.contains(c.uniqueKey))
    }

    val generalComment = createGeneralComment(pullRequest, git, tmpDir, diffModel.keySet)

    git.clean()

    (newPrComments, generalComment)
  }


  private def createGeneralComment(prModel: PullRequest, git: GitApi, tmpDir: String, filter: Set[String]): String = {
    git.changeBranch(prModel.intoBranch)

    val before = checkers.map { check =>
      (check.description, check.execute(tmpDir, filter).toSet.size)
    }.toMap

    git.merge(prModel.fromBranch)

    val checksDiff = checkers.map { check =>
      (check.description, check.execute(tmpDir, filter).toSet.size - before(check.description))
    }.toMap

    val karma = if (checksDiff.values.sum < 0) s"@${prModel.author}++ for making code more clean" else ""

    val generalComment = checksDiff.foldLeft("") { case (result, (check, diff)) =>
      result + s"$check violations diff: ${if (diff < 0) "" else "+"}$diff\n"
    }
    generalComment + karma
  }
}
