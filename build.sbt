organization := "ru.bugzmanov"

name := "pr-check"

version := "0.0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.googlecode.java-diff-utils" % "diffutils" % "1.2.1",
  "net.sourceforge.pmd" %	"pmd-java" %	"5.2.3",
  "net.sourceforge.pmd" % "pmd-core" % "5.2.3",
  "com.puppycrawl.tools" % "checkstyle" % "6.4.1",
  "com.google.code.findbugs" % "findbugs" % "3.0.1",
//  "org.codehaus.sonar-plugins.java" % "java-checks" % "3.0",
//  "org.codehaus.sonar" % "sonar-java-api" % "3.0",
//  "org.codehaus.sonar" % "sonar-plugin-api" % "3.0",
//  "org.codehaus.sonar-plugins.java" % "sonar-java-plugin" % "3.0",
  "com.jcabi" % "jcabi-github" % "0.21.3",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "3.7.0.201502260915-r",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test"
)