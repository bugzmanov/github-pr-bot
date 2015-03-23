package ru.bugzmanov.prcheck

import java.io.File

import org.slf4j.LoggerFactory
import ru.bugzmanov.prcheck.checks.{CheckstyleExecutor, ViolationChecker}

class CodeReviewer(botuser: String, checkers: Seq[ViolationChecker]) {

  val logger = LoggerFactory.getLogger("CodeReviewer")

  def collectReviewComments(githubApi: GithubApi, prNumber: Int): Either[String, (Vector[Comment], String)] = {
    val tmpDir: String = s"/tmp/github-${githubApi.repo}-${System.currentTimeMillis()}/"
    val prId: Int = prNumber.toInt
    val tmpDirFile: File = new File(tmpDir)

    logger.debug(s"Downloading PR $prNumber diff")

    val pullRequest = githubApi.describePR(prId)

    val diffContent: String = githubApi.downloadDiff(prId)

    println("---------\n" + diffContent + "\n----------------")

    val diffModel: Map[String, Patch] = DiffParser.
      parseMultiDiff(diffContent.split("\n").toList).map(
        k => (new File(tmpDir + k.revised).getCanonicalPath, k)
      ).toMap

    logger.debug(s"Clonning git repo for PR $prNumber")
    val git = githubApi.cloneTo(tmpDirFile, pullRequest.fromBranch)

    logger.debug(s"Running violation checkers for PR $prNumber")
    val violations = checkers.flatMap { check =>
      check.execute(tmpDir, diffModel.keySet)
        .filter(issue => diffModel.get(issue.file).exists(_.isWithinRevisedVersion(issue.line)))
    }

    val requiredPrComments = violations.groupBy(_.file).flatMap { case (file, viols) =>
      val (tabChecks, other) = viols.partition(_.rule == CheckstyleExecutor.tabCheck) // to prevent massive comments about tabs
      other ++ (if (tabChecks.nonEmpty)
        Seq(tabChecks.head.copy(description = s"Achtung! ${tabChecks.size} lines with tab characters in this PR in this file"))
      else Seq())
    }.map { f =>
      val relativePath = f.file.substring(tmpDirFile.getCanonicalPath.size + 1)
      val position = diffModel(f.file).mapToDiffPosition(f.line)
      Comment(prId, relativePath, position, pullRequest.fromCommit, botuser, s"[${f.tag}] ${f.description}")
    }.toVector

    val newPrComments = {
      val oldBotComments = githubApi.getCommentsByUser(prNumber, botuser).map(_.uniqueKey).toSet
      requiredPrComments.filterNot(c => oldBotComments.contains(c.uniqueKey))
    }

    val generalComment = if (pullRequest.isMergeable) {
      createGeneralComment(pullRequest, git, tmpDir, diffModel.keySet)
    } else {
      "Can't calculate violations diff because pull request can't be merged automatically"
    }

    git.clean()

    Right(newPrComments, generalComment)
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
