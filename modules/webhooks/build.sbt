name := "webhooks"

scalaVersion in ThisBuild := "2.11.4"

libraryDependencies ++= Seq(
  "org.mockito" % "mockito-core" % "1.+" % Test,
  "org.scalatestplus" %% "play" % "1.1.0" % Test
//"org.specs2" %% "specs2-core" % "3.0.1" % Test
)
