organization := "org.make"
name := "openstack-swift-client"
version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.16",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.16",
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
)