/*
 * Copyright 2018 Make.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

organization := "org.make"
name := "openstack-swift-client"

scalaVersion := "2.12.6"

licenses := Seq("Apache 2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0"))

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-actor"    % "2.5.17",
  "com.typesafe.akka"          %% "akka-stream"   % "2.5.17",
  "com.typesafe.akka"          %% "akka-http"     % "10.1.5",
  "io.circe"                   %% "circe-core"    % circeVersion,
  "io.circe"                   %% "circe-generic" % circeVersion,
  "io.circe"                   %% "circe-parser"  % circeVersion,
  "io.circe"                   %% "circe-java8"   % circeVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "com.typesafe"               % "config"         % "1.3.2",
  // Test dependencies
  "org.slf4j"     % "slf4j-simple"                     % "1.7.25" % "test",
  "org.scalatest" %% "scalatest"                       % "3.0.5"  % "test",
  "com.whisk"     %% "docker-testkit-scalatest"        % "0.9.6"  % "test",
  "com.whisk"     %% "docker-testkit-impl-docker-java" % "0.9.6"  % "test",
  "org.mockito"   %% "mockito-scala"                   % "0.4.5"  % "test"
)

Test / parallelExecution := false

developers := List(
  Developer(
    id = "flaroche",
    name = "François LAROCHE",
    email = "fl@make.org",
    url = url("https://github.com/larochef")
  ),
  Developer(
    id = "cpestoury",
    name = "Charley PESTOURY",
    email = "cp@make.org",
    url = url("https://gitlab.com/cpestoury")
  ),
  Developer(
    id = "csalmon-legagneur",
    name = "Colin SALMON-LEGAGNEUR",
    email = "salmonl.colin@gmail.com",
    url = url("https://gitlab.com/csalmon-")
  ),
  Developer(id = "pda", name = "Philippe de ARAUJO", email = "pa@make.org", url = url("https://gitlab.com/philippe.da"))
)

scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://gitlab.com/makeorg/platform/openstack-swift-client"),
    connection = "scm:git:git://gitlab.com:makeorg/platform/openstack-swift-client.git",
    devConnection = Some("scm:git:ssh://gitlab.com:makeorg/platform/openstack-swift-client.git")
  )
)

startYear := Some(2018)

organizationHomepage := Some(url("https://make.org"))
homepage := Some(url("https://gitlab.com/makeorg/platform/openstack-swift-client"))

pomIncludeRepository := { _ =>
  false
}

publishMavenStyle := true

publishArtifact in Test := false

releasePublishArtifactsAction := PgpKeys.publishSigned.value

pgpPassphrase := {
  val password: String = System.getenv("GPG_PASSWORD")
  password match {
    case null =>
      None
    case "" =>
      None
    case other =>
      Some(other.trim.toCharArray)
  }
}

publishTo := {
  if (isSnapshot.value) {
    Some("releases".at("https://oss.sonatype.org/content/repositories/snapshots"))
  } else {
    Some("snapshots".at("https://oss.sonatype.org/service/local/staging/deploy/maven2"))
  }
}
