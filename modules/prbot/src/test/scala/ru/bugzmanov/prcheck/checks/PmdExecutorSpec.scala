package ru.bugzmanov.prcheck.checks

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.Charset

import net.sourceforge.pmd.util.datasource.DataSource
import org.scalatest.FlatSpec
import org.scalatest.Matchers._


class PmdExecutorSpec extends FlatSpec {
  import AsDataSource.fromString
  val HelloWorldIssues = Seq("UseUtilityClass", "UseVarargs", "SystemPrintln", "NoPackage")
  val executor = JavaPmdExecutor.fromRulesFile("pmd_rules.xml")

  val BadHelloWorldCode =
    """
      |public class HelloWorld {
      |
      |    public static void main(String[] args) {
      |        System.out.println("Hello, World");
      |    }
      |
      |}
    """.stripMargin

  val GoodHelloWorldCode =
    """
      |package foo.bar;
      |
      |public final class HelloWorld {
      |
      |    private HelloWorld() { /* so PMD will think it's utility class */ }
      |
      |    public static void main(String ... args) {
      |        log.info("Hello, World");
      |    }
      |
      |}
    """.stripMargin


  "Pmd suite" should "report issues for problematic version of 'Hello world'" in {
    val issues = executor.process(fromString(BadHelloWorldCode)).get
    issues.map(_.rule) should contain theSameElementsAs HelloWorldIssues
  }
  it should "remain silent for linted version of 'Hello world'" in {
    val issues = executor.process(fromString(GoodHelloWorldCode)).get
    issues shouldBe empty
  }

}

object AsDataSource {
  private final val Utf8 = Charset.forName("UTF-8")
  def fromString(str: String): DataSource = new DataSource {
    override def getNiceFileName(shortNames: Boolean, inputFileName: String): String = "made-from-string"
    override def getInputStream: InputStream = new ByteArrayInputStream(str.getBytes(Utf8))
  }
}
