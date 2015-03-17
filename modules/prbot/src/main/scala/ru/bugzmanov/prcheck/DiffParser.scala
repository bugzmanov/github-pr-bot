package ru.bugzmanov.prcheck

import java.util
import java.util.regex.{Matcher, Pattern}

import scala.annotation.tailrec
import scala.collection.JavaConversions._


case class Chunk(position: Int, lines: Vector[String])

sealed trait Delta {
  def original: Chunk
  def revised: Chunk
}

case class ChangeDelta (original: Chunk, revised: Chunk) extends Delta

case class Patch(
  revisionSHA:String,
  original: String,
  revised:String,
  deltas: Seq[Delta]) {

  def isFileCreation: Boolean = original == ""
  def isFileDeletion: Boolean = revised == ""

  def isWithinRevisedVersion(lineNumber: Int): Boolean = {
    deltas.exists(d => lineNumber >= d.revised.position && lineNumber <= d.revised.lines.size + d.revised.position)
  }
}

object DiffParser {

  implicit class AddMultispanToList[A](val list: List[A]) extends AnyVal {
    def multiSpan(splitOn: (A) => Boolean): List[List[A]] = {
      @tailrec
      def loop(xs: List[A], acc: List[List[A]]) : List[List[A]] = xs match {
        case Nil => acc
        case x :: Nil => List(x) :: acc
        case h :: t =>
          val (pre,post) = t.span(!splitOn(_))
          loop(post, (h :: pre) :: acc)
      }
      loop(list, Nil).reverse
    }
  }

  private val UnifiedDiffChunkRe = Pattern.compile("^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@.*")
  private val originFileNameR = "\\-\\-\\- a/(.*)".r
  private val revisedFileNameR = "\\+\\+\\+ b/(.*)".r
  private val revisionR = "index (.*)\\.\\.([a-zA-Z0-9]+) .*".r
  private val revisionR2 = "index (.*)\\.\\.([a-zA-Z0-9]+)".r

  def parseMultiDiff(diff: List[String]): List[Patch] = {
    val span: List[List[String]] = diff.multiSpan(_.startsWith("diff --git")).dropWhile(_.size == 1)

    span.map(parseUnifiedDiff)
  }

  def parseUnifiedDiff(diff: List[String]): Patch = {
    var inPrelude: Boolean = true
    val rawChunk = new util.ArrayList[(String, String)]
    val patch = Vector.newBuilder[Delta]
    var old_ln: Int = 0
    var new_ln: Int = 0
    var originalFileName = ""
    var revisedFileName = ""
    var revision = "0000000"
    for (line <- diff) {
      if (inPrelude) {
        line match {
          case originFileNameR(name) => originalFileName = name
          case revisedFileNameR(name) =>
            revisedFileName = name
            inPrelude = false
          case revisionR(_, revisionSHA) => revision = revisionSHA
          case revisionR2(_, revisionSHA) => revision = revisionSHA

          case _ => //do nothing
        }
      } else {
        val m: Matcher = UnifiedDiffChunkRe.matcher(line)
        if (m.find) {
          if (rawChunk.size != 0) {
            val (oldchunk, newchunk) = collectRawChunk(rawChunk)
            patch += new ChangeDelta(new Chunk(old_ln, oldchunk), new Chunk(new_ln, newchunk))
            rawChunk.clear()
          }
          old_ln = Option(m.group(1)).map(_.toInt).getOrElse(1)
          new_ln = Option(m.group(3)).map(_.toInt).getOrElse(1)
        }
        else {
          if (line.length > 0) {
            val (tag, rest)  = line.splitAt(1)
            if ((tag == " ") || (tag == "+") || (tag == "-")) {
              rawChunk.add((tag, rest))
            }
          }
          else {
            rawChunk.add((" ", ""))
          }
        }
      }
    }
    if (rawChunk.size != 0) {
      val (oldchunk, newchunk) = collectRawChunk(rawChunk)
      patch += new ChangeDelta(new Chunk(old_ln , oldchunk), new Chunk(new_ln, newchunk))
    }
    new Patch(revision, original = originalFileName, revised = revisedFileName, patch.result())
  }

  private def collectRawChunk(rawChunk: util.ArrayList[(String, String)]): (Vector[String], Vector[String]) =
    rawChunk.foldLeft((Vector[String](), Vector[String]())) { case ((oldc, newc), (tag, rest)) =>
      tag match {
        case " " => (oldc :+ rest, newc :+ rest)
        case "-" => (oldc :+ rest, newc)
        case "+" => (oldc, newc :+ rest)
        case _ => (oldc, newc)
      }
    }
}
