package org.make.swift

import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientConfig}
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory
import com.whisk.docker.impl.dockerjava.{Docker, DockerJavaExecutorFactory}
import com.whisk.docker.{DockerContainer, DockerFactory, DockerKit, DockerReadyChecker}

trait DockerSwiftAllInOne extends DockerKit {

  private val internalPort = 8080

  def externalPort: Option[Int] = None

  private def swiftContainer: DockerContainer =
    DockerContainer(image = "bouncestorage/swift-aio", name = Some(getClass.getSimpleName))
      .withPorts(internalPort -> externalPort)
      .withReadyChecker(DockerReadyChecker.LogLineContains("supervisord started with pid"))

  override def dockerContainers: List[DockerContainer] = swiftContainer :: super.dockerContainers

  private val dockerClientConfig: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
  private val client: Docker = new Docker(dockerClientConfig, new JerseyDockerCmdExecFactory())
  override implicit val dockerFactory: DockerFactory = new DockerJavaExecutorFactory(client)
}
