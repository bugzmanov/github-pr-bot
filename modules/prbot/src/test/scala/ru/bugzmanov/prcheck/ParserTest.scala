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

  "A parser" should "should recognise file name in diff" in {
    val Patch(_, original, revised, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    assert(original === "src/main/java/com/EntityImpl.java", "Original File name extracted from diff")
    assert(revised === "src/main/java/com/EntityImpl.java", "revised File name extracted from diff")
  }

  "A parser" should "recognise single block of changes" in {
    val Patch(revision, original, revised, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas should have size (1)

    deltas.head.original.position should be (originalPosition)
    deltas.head.revised.position should be (revisedPosition)
    revision should be (revisedSHA)

  }

  "Original chunk" should "contain deleted part" in {
    val Patch(_, _, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.original.lines.map(_.trim) should contain("public void setAdvEntityId(long id){")
  }

  "Revised chunk" should "not contain deleted part" in {
    val Patch(_, _, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.revised.lines.map(_.trim) should not contain "public void setAdvEntityId(long id){"
  }

  "Original chunk" should "not contain new part" in {
    val Patch(_ ,_, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.original.lines.map(_.trim) should not contain "public EntityImpl setAdvEntityId(long id){"
    deltas.head.original.lines.map(_.trim) should not contain "return this;"
  }

  "Revised chunk" should  "contain new part" in {
    val Patch(_, _, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.revised.lines.map(_.trim) should contain ("public EntityImpl setAdvEntityId(long id){")
    deltas.head.revised.lines.map(_.trim) should contain ("return this;")
  }

  "Revised and original chunks" should "contain unmodified parts" in {
    val Patch(_, _, _, deltas) = DiffParser.parseUnifiedDiff(diff.split("\n").toList)
    deltas.head.revised.lines.map(_.trim) should contain ("this.advEntityId=id;")
    deltas.head.original.lines.map(_.trim) should contain ("this.advEntityId=id;")
  }

  "a parser" should "recognise several deltas" in {
    val largeDiff = diff +
      """
        |@@ -100,8 +105,9 @@
        |
        |- blabla
        |+ ne_albalb
      """.stripMargin

    val Patch(_, _, _, deltas) = DiffParser.parseUnifiedDiff(largeDiff.split("\n").toList)
    deltas should have size (2)

    deltas(1).original.lines.map(_.trim) should contain("blabla")
    deltas(1).revised.lines.map(_.trim) should contain("ne_albalb")

  }
}
