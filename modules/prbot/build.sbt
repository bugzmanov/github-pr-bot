name := "prbot"

scalaVersion in ThisBuild := "2.11.4"

libraryDependencies ++= Seq(
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
  "org.scalatest" %% "scalatest" % "2.1.3" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.1" % "test"
)