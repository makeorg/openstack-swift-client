organization := "org.make"
name := "openstack-swift-client"
version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.16",
  "com.typesafe.akka" %% "akka-stream" % "2.5.16",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.16",
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
  "io.circe" %% "circe-core" % "0.10.0",
  "io.circe" %% "circe-generic" % "0.10.0",
  "io.circe" %% "circe-parser" % "0.10.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.slf4j" % "slf4j-simple" % "1.7.25" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.6" % "test",
  "com.whisk" %% "docker-testkit-impl-docker-java" % "0.9.6" % "test",
  "org.mockito" %% "mockito-scala" % "0.4.5" % "test"
)