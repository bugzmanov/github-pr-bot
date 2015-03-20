name := "webhooks"

scalaVersion in ThisBuild := "2.11.4"

libraryDependencies ++= Seq(
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.mockito" % "mockito-core" % "1.+" % Test,
  "org.scalatestplus" %% "play" % "1.1.0" % Test
)
