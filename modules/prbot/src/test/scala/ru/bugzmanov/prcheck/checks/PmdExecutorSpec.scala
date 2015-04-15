package ru.bugzmanov.prcheck.checks

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.Charset

import net.sourceforge.pmd.util.datasource.DataSource
import org.scalatest.Matchers._
import org.scalatest.{FlatSpec, TryValues}


class PmdExecutorSpec extends FlatSpec with TryValues {
  import StringDataSource.fromString
  val HelloWorldIssues = Seq("UseUtilityClass", "UseVarargs", "SystemPrintln", "NoPackage")

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

  val UnparseableCode = fromString(
    name = "unparseable",
    contents =
      """
        | blah blah
      """.stripMargin
  )


  "Pmd suite" should "report issues for problematic version of 'Hello world'" in {
    val executor = JavaPmdExecutor.fromRulesFile("pmd_rules.xml")
    val issues = executor.analyze(BadHelloWorldCode).success.value
    issues.map(_.rule) should contain theSameElementsAs HelloWorldIssues
  }
  it should "remain silent for linted version of 'Hello world'" in {
    val executor = JavaPmdExecutor.fromRulesFile("pmd_rules.xml")
    val issues = executor.analyze(GoodHelloWorldCode).success.value
    issues shouldBe empty
  }
  it should "correctly process multiple files at once" in {
    val executor = JavaPmdExecutor.fromRulesFile("pmd_rules.xml")
    val issues = executor.analyze(BadHelloWorldCode).success.value
    val noIssues = executor.analyze(GoodHelloWorldCode).success.value

    noIssues shouldBe empty
    issues.map(_.rule) should contain theSameElementsAs HelloWorldIssues
  }
  it should "throw IllegalArgumentException in case of configuration errors" in {
    val executor = JavaPmdExecutor.fromRulesFile("broken_rules.xml")
    val failure = executor.analyze(GoodHelloWorldCode).failure

    failure.exception match {
      case ex: IllegalArgumentException => ex.getMessage should startWith("Configuration errors:")
      case _ => fail("Expected IAS for misconfigured rules file")
    }
  }
  it should "throw IllegalStateException in case of processing errors" in {
    val executor = JavaPmdExecutor.fromRulesFile("pmd_rules.xml")
    val failure = executor.analyze(UnparseableCode).failure

    failure.exception match {
      case ex: IllegalStateException => ex.getMessage should startWith("Caught error(s) during processing: ")
      case _ => fail("Expected RuntimeException for unparseable files")
    }
  }
}

private object StringDataSource {
  private final val Utf8 = Charset.forName("UTF-8")
  def fromString(name: String, contents: String): DataSource = new DataSource {
    override def getNiceFileName(shortNames: Boolean, inputFileName: String): String = name
    override def getInputStream: InputStream = new ByteArrayInputStream(contents.getBytes(Utf8))
  }
}
