package ru.bugzmanov.prcheck.checks

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.Charset

import net.sourceforge.pmd.util.datasource.DataSource
import org.scalatest.FlatSpec
import org.scalatest.Matchers._


class PmdExecutorSpec extends FlatSpec {
  val HelloWorldCode =
    """
      |public class HelloWorld {
      |
      |    public static void main(String[] args) {
      |        System.out.println("Hello, World");
      |    }
      |
      |}
    """.stripMargin

  val HelloWorldIssues = Seq("UseUtilityClass", "UseVarargs", "SystemPrintln", "NoPackage")
  val executor = JavaPmdExecutor.fromRulesFile("pmd_rules.xml")

  "Pmd suite" should s"report issues for 'Hello world' code piece" in {
    val issues = executor.process(AsDataSource.fromString(HelloWorldCode)).get
    issues.map(_.rule) should contain theSameElementsAs HelloWorldIssues
  }
  ignore should "yield errors if file is missing" in {

  }
}

object AsDataSource {
  private final val Utf8 = Charset.forName("UTF-8")
  def fromString(str: String): DataSource = new DataSource {
    override def getNiceFileName(shortNames: Boolean, inputFileName: String): String = "made-from-string"
    override def getInputStream: InputStream = new ByteArrayInputStream(str.getBytes(Utf8))
  }
}
