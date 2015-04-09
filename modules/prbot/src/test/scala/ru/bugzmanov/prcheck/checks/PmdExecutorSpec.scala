package ru.bugzmanov.prcheck.checks

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.Charset

import net.sourceforge.pmd.util.datasource.DataSource
import org.scalatest.FlatSpec
import org.scalatest.Matchers._


class PmdExecutorSpec extends FlatSpec {
  import StringDataSource.fromString
  val HelloWorldIssues = Seq("UseUtilityClass", "UseVarargs", "SystemPrintln", "NoPackage")
  val executor = JavaPmdExecutor.fromRulesFile("pmd_rules.xml")

  val BadHelloWorldCode = fromString(
    name = "bad-hello-word",
    contents = """
    |public class HelloWorld {
    |
    |    public static void main(String[] args) {
    |        System.out.println("Hello, World");
    |    }
    |}
    """.stripMargin
  )

  val GoodHelloWorldCode = fromString(
    name = "good-hello-world",
    contents =
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
      |}
      """.stripMargin
  )


  "Pmd suite" should "report issues for problematic version of 'Hello world'" in {
    val issues = executor.process(BadHelloWorldCode).get
    issues.map(_.rule) should contain theSameElementsAs HelloWorldIssues
  }
  it should "remain silent for linted version of 'Hello world'" in {
    val issues = executor.process(GoodHelloWorldCode).get
    issues shouldBe empty
  }
  it should "correctly process multiple files at once" in {
    val issues = executor.process(BadHelloWorldCode, GoodHelloWorldCode).get
    issues.map(_.rule) should contain theSameElementsAs HelloWorldIssues
  }
}

private object StringDataSource {
  private final val Utf8 = Charset.forName("UTF-8")
  def fromString(name: String, contents: String): DataSource = new DataSource {
    override def getNiceFileName(shortNames: Boolean, inputFileName: String): String = name
    override def getInputStream: InputStream = new ByteArrayInputStream(contents.getBytes(Utf8))
  }
}
