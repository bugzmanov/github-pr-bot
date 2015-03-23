package ru.bugzmanov.prcheck

import org.scalatest.FlatSpec
import org.scalatest._
import Matchers._

class ParserTest extends FlatSpec {
  val originalPosition: Int = 20
  val revisedPosition: Int = 21
  val revisedSHA = "d70032e"
  val diff =
    s"""
      |diff --git a/src/main/java/com/EntityImpl.java b/src/main/java/com/EntityImpl.java
      |index 6244133..$revisedSHA 100644
      |--- a/src/main/java/com/EntityImpl.java
      |+++ b/src/main/java/com/EntityImpl.java
      |@@ -$originalPosition,8 +$revisedPosition,9 @@ public long getAdvEntityId() {
      | 		return advEntityId;
      | 	}
      |
      |-	public void setAdvEntityId(long id){
      |+	public EntityImpl setAdvEntityId(long id){
      | 		this.advEntityId=id;
      |+		return this;
      |
    """.stripMargin

  def trimLine: PartialFunction[(String, Int), String] = { case (line: String, position: Int) => line.trim }

  "A parser" should "should recognise file name in diff" in {
    val Patch(original, revised, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    assert(original === "src/main/java/com/EntityImpl.java", "Original File name extracted from diff")
    assert(revised === "src/main/java/com/EntityImpl.java", "revised File name extracted from diff")
  }

  "A parser" should "recognise single block of changes" in {
    val Patch(original, revised, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas should have size (1)

    deltas.head.original.position should be (originalPosition)
    deltas.head.revised.position should be (revisedPosition)
  }

  "Original chunk" should "contain deleted part" in {
    val Patch(_, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.original.lines.map(trimLine) should contain("public void setAdvEntityId(long id){")
  }

  "Revised chunk" should "not contain deleted part" in {
    val Patch(_, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.revised.lines.map(trimLine) should not contain "public void setAdvEntityId(long id){"
  }

  "Original chunk" should "not contain new part" in {
    val Patch(_, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.original.lines.map(trimLine) should not contain "public EntityImpl setAdvEntityId(long id){"
    deltas.head.original.lines.map(trimLine) should not contain "return this;"
  }

  "Revised chunk" should  "contain new part" in {
    val Patch(_, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.revised.lines.map(trimLine) should contain ("public EntityImpl setAdvEntityId(long id){")
    deltas.head.revised.lines.map(trimLine) should contain ("return this;")
  }

  "Revised and original chunks" should "contain unmodified parts" in {
    val Patch(_, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.revised.lines.map(trimLine) should contain ("this.advEntityId=id;")
    deltas.head.original.lines.map(trimLine) should contain ("this.advEntityId=id;")
  }

  "a parser" should "recognise several deltas" in {
    val largeDiff = diff +
      """
        |@@ -100,8 +105,9 @@
        |
        |- blabla
        |+ ne_albalb
      """.stripMargin

    val Patch(_, _, deltas) = DiffParser.parseUnifiedDiff(largeDiff.split("\n").toList)
    deltas should have size (2)

    deltas(1).original.lines.map(trimLine) should contain("blabla")
    deltas(1).revised.lines.map(trimLine) should contain("ne_albalb")
  }


  "Patch" should "be able to map line number back to index" in {
    val diff =
      """
        |diff --git a/src/main/java/com/textmagic/sms/TextMagicMessageService.java b/src/main/java/com/textmagic/sms/TextMagicMessageService.java
        |index e9a8332..07b694d 100644
        |--- a/src/main/java/com/textmagic/sms/TextMagicMessageService.java
        |+++ b/src/main/java/com/textmagic/sms/TextMagicMessageService.java
        |@@ -28,7 +28,8 @@
        |  *
        |  * @author Rafael Bagmanov
        |  */
        |-public class TextMagicMessageService implements MessageService {
        |+public class TextMagicMessageService implements MessageService
        |+{
        |
        |     // universal constants
        |     private static final int MAX_SMS_PARTS_COUNT = 3;
        |@@ -99,7 +100,8 @@ public void setParser(TextMagicResponseParser parser) {
        |      */
        |     public SentMessage send(String text, String phone) throws ServiceBackendException, ServiceTechnicalException{
        |         List<SentMessage> list = send(text, Arrays.asList(phone));
        |-        if (list.size() != 1) {
        |+        if (list.size() != 1)
        |+	{
        |             throw new ServiceTechnicalException("The server response is unexpected. " +
        |                     "The response object was not populated with single result: [" + Arrays.toString(list.toArray()) + "]");
        |   }
        |
      """.stripMargin

    val patch: Patch = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    patch.mapToDiffPosition(28) should be (1)
    patch.mapToDiffPosition(101) should be (12)
  }

  "massiveTest" should "work" in {
    val diff =
      """
        |diff --git a/src/main/java/com/textmagic/sms/TextMagicMessageService.java b/src/main/java/com/textmagic/sms/TextMagicMessageService.java
        |index e9a8332..07b694d 100644
        |--- a/src/main/java/com/textmagic/sms/TextMagicMessageService.java
        |+++ b/src/main/java/com/textmagic/sms/TextMagicMessageService.java
        |@@ -28,7 +28,8 @@
        |  *
        |  * @author Rafael Bagmanov
        |  */
        |-public class TextMagicMessageService implements MessageService {
        |+public class TextMagicMessageService implements MessageService
        |+{
        |
        |     // universal constants
        |     private static final int MAX_SMS_PARTS_COUNT = 3;
        |@@ -99,7 +100,8 @@ public void setParser(TextMagicResponseParser parser) {
        |      */
        |     public SentMessage send(String text, String phone) throws ServiceBackendException, ServiceTechnicalException{
        |         List<SentMessage> list = send(text, Arrays.asList(phone));
        |-        if (list.size() != 1) {
        |+        if (list.size() != 1)
        |+	{
        |             throw new ServiceTechnicalException("The server response is unexpected. " +
        |                     "The response object was not populated with single result: [" + Arrays.toString(list.toArray()) + "]");
        |         }
        |@@ -177,7 +179,8 @@ public SentMessage send(String text, String phone) throws ServiceBackendExceptio
        |         if(maxLength > 3 || maxLength < 1) {
        |             throw new IllegalArgumentException("maxLength value is invalid");
        |         }
        |-        if (sendingTime.getTime() < System.currentTimeMillis()) {
        |+        if (sendingTime.getTime() < System.currentTimeMillis())
        |+{
        |             throw new IllegalArgumentException("Provided sendingTime value [" + sendingTime.toString() + "] is in the past");
        |         }
        |         boolean useUnicode = !GsmCharsetUtil.isLegalString(text);
      """.stripMargin

    val patch: Patch = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
  }
}
