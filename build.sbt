organization in ThisBuild := "ru.bugzmanov"

version := "0.0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.4"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-language:higherKinds")

javacOptions in ThisBuild ++= Seq("-source", "1.7", "-target", "1.7")

lazy val root = (project in file(".")).aggregate(
  prbot,
  webhooks
)

lazy val prbot = project in file("modules/prbot")

lazy val webhooks = project.in(file("modules/webhooks"))
  .dependsOn(prbot)
  .enablePlugins(PlayScala)

