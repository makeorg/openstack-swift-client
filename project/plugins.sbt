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

addSbtPlugin("com.geirsson"      % "sbt-scalafmt"           % "1.2.0")
addSbtPlugin("org.scalastyle"    %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"          % "1.6.0")
addSbtPlugin("com.codacy"        % "sbt-codacy-coverage"    % "1.3.14")
addSbtPlugin("com.github.gseitz" % "sbt-release"            % "1.0.9")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"                % "1.1.1")

classpathTypes += "maven-plugin"
