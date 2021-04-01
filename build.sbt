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
import com.jsuereth.sbtpgp.PgpKeys._

organization := "org.make"
name := "openstack-swift-client"

scalaVersion := "2.13.1"

licenses := Seq("Apache 2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0"))

crossScalaVersions := Seq("2.12.13", "2.13.5")

val circeVersion = "0.13.0"
val akkaVersion = "2.6.13"
val dockerTestkitVersion = "0.9.9"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"  % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-http"         % "10.2.4",
  "io.circe"          %% "circe-core"        % circeVersion,
  "io.circe"          %% "circe-generic"     % circeVersion,
  "io.circe"          %% "circe-parser"      % circeVersion,
  "com.typesafe"      % "config"             % "1.4.1",
  // Test dependencies
  "org.slf4j"                  % "slf4j-simple"                     % "1.7.30"             % Test,
  "com.typesafe.scala-logging" %% "scala-logging"                   % "3.9.3"              % Test,
  "org.scalatest"              %% "scalatest"                       % "3.2.7"              % Test,
  "com.whisk"                  %% "docker-testkit-scalatest"        % dockerTestkitVersion % Test,
  "com.whisk"                  %% "docker-testkit-impl-docker-java" % dockerTestkitVersion % Test,
  "org.mockito"                %% "mockito-scala"                   % "1.16.33"            % Test
)

Test / parallelExecution := false

scalacOptions ++= Seq(
  "-Yrangepos",
  "-Xlint",
  "-deprecation",
  "-Xfatal-warnings",
  "-feature",
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-unused",
  "-language:_",
  "-Ycache-plugin-class-loader:last-modified",
  "-Ycache-macro-class-loader:last-modified",
  "-Ybackend-parallelism",
  "5"
)

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

Global / gpgCommand := (baseDirectory.value / "gpg.sh").getAbsolutePath
