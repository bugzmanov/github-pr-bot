package ru.bugzmanov.prcheck.checks

import java.io.{Writer, FileWriter, File}

import org.scalatest.FlatSpec
import org.scalatest._
import Matchers._

class CheckstyleExecutorSmokeTest extends FlatSpec with BeforeAndAfterEach {

  private val tempFile = File.createTempFile("Blabla", ".java")
  private var writer: Writer = _

  "checkstyle config" should "not break CheckstyleExecutor" in {

    writer.write(
      """
        |class Blabla{}
      """.stripMargin)
    writer.close()

    val result = CheckstyleExecutor.execute(tempFile.getParent, Set(tempFile.getCanonicalPath))
    result should have size 1
  }

  override protected def beforeEach(): Unit = {
    writer = new FileWriter(tempFile)
  }

  override protected def afterEach(): Unit = {
    try {
      writer.close()
    } finally {
      tempFile.delete()
    }
  }
}
