package ru.bugzmanov.prcheck

import java.util.regex.Pattern

import scala.annotation.tailrec


case class Chunk(position: Int, lines: Vector[(String, Int)]) {
  def containsLine(lineNumber: Int) = lineNumber >= position && lineNumber < lines.size + position
  def mapToPosition(lineNumber: Int) = lines(lineNumber - position)._2
}

sealed trait Delta {
  def original: Chunk
  def revised: Chunk
}

case class ChangeDelta(original: Chunk, revised: Chunk) extends Delta

case class Patch(
  original: String,
  revised: String,
  deltas: Seq[Delta]) {

  val isFileCreation: Boolean = original.isEmpty
  val isFileDeletion: Boolean = revised.isEmpty

  def isWithinRevisedVersion(lineNumber: Int): Boolean = {
    deltas.exists(_.revised.containsLine(lineNumber))
  }

  def mapToDiffPosition(revisedLine: Int): Int = deltas
    .find(_.revised.containsLine(revisedLine))
    .map(_.revised.mapToPosition(revisedLine))
    .get //todo
}

object DiffParser {

  implicit class AddMultispanToList[A](val list: List[A]) extends AnyVal {
    def multiSpan(splitOn: (A) => Boolean): List[List[A]] = {
      @tailrec
      def loop(xs: List[A], acc: List[List[A]]): List[List[A]] = xs match {
        case Nil => acc
        case x :: Nil => List(x) :: acc
        case h :: t =>
          val (pre, post) = t.span(!splitOn(_))
          loop(post, (h :: pre) :: acc)
      }
      loop(list, Nil).reverse
    }
  }

  private val UnifiedDiffChunkRe = Pattern.compile("^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@.*")

  private val UnifiedDiffChunk = "^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@.*".r
  private val originFileNameR = "\\-\\-\\- a/(.*)".r
  private val revisedFileNameR = "\\+\\+\\+ b/(.*)".r

  def parseMultiDiff(diff: List[String]): List[Patch] = {
    val span = diff.dropWhile(!_.startsWith("diff --git")).multiSpan(_.startsWith("diff --git")).dropWhile(_.size == 1)
    span.map(parseUnifiedDiff)
  }

  def parseUnifiedDiff(diff: List[String]): Patch = {
    val diffs = diff.multiSpan(UnifiedDiffChunkRe.matcher(_).find)

    val (originalFileName, revisedFileName) = {
      val r = diffs.head.collect {
        case originFileNameR(name) => "original" -> name
        case revisedFileNameR(name) => "revised" -> name
      }.toMap
      (r("original"), r("revised"))
    }

    val split = diffs.tail.reduce(_ ++ _).zipWithIndex.multiSpan { case (line, index) => UnifiedDiffChunkRe.matcher(line).find}

    val deltas = for (chunk <- split) yield {
      val UnifiedDiffChunk(origLinIdx, _, revisedLineIdx, _) = chunk.head._1
      val empty = Vector[(String, Int)]()

      val (original, revised) = chunk.tail.foldLeft((empty, empty)) { case ((oldc, newc), (line, position)) =>
        val (tag, rest) = line.splitAt(1)
        tag match {
          case " " => (oldc :+(rest, position), newc :+(rest, position))
          case "-" => (oldc :+(rest, position), newc)
          case "+" => (oldc, newc :+(rest, position))
          case _ => (oldc, newc)
        }
      }
      new ChangeDelta(new Chunk(origLinIdx.toInt, original), new Chunk(revisedLineIdx.toInt, revised))
    }
    new Patch(original = originalFileName, revised = revisedFileName, deltas)
  }
}
