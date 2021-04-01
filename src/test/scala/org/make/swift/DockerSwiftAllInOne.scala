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

package org.make.swift

import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientConfig}
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import com.whisk.docker.impl.dockerjava.{Docker, DockerJavaExecutorFactory, DockerKitDockerJava}
import com.whisk.docker.{DockerContainer, DockerFactory, DockerKit, DockerReadyChecker}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait DockerSwiftAllInOne extends DockerKit with DockerKitDockerJava {

  private val internalPort = 8080

  def externalPort: Option[Int] = None

  private def swiftContainer: DockerContainer =
    DockerContainer(image = "bouncestorage/swift-aio", name = Some(getClass.getSimpleName))
      .withPorts(internalPort -> externalPort)
      .withReadyChecker(DockerReadyChecker.LogLineContains("supervisord started with pid"))

  override val StartContainersTimeout: FiniteDuration = 5.minutes
  override val StopContainersTimeout: FiniteDuration = 1.minute

  override def dockerContainers: List[DockerContainer] = swiftContainer :: super.dockerContainers

  private val dockerClientConfig: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
  private val factory = new NettyDockerCmdExecFactory()
  private val client: Docker = new Docker(dockerClientConfig, factory)
  override implicit val dockerFactory: DockerFactory = new DockerJavaExecutorFactory(client)

  def close(): Unit = {
    factory.close()
  }
}
