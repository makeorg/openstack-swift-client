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

import akka.actor.typed.ActorSystem

import java.io.{ByteArrayOutputStream, File, FileInputStream, InputStream}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import com.typesafe.config.Config
import org.make.swift.authentication.AuthenticationActor.AuthenticationActorProps
import org.make.swift.model.{Bucket, Resource}
import org.make.swift.storage.ActorBasedSwiftClient

import scala.concurrent.Future
import scala.util.{Success, Try}

trait SwiftClient {

  def init(): Future[Unit]
  def getSwiftPath: Future[String]
  def downloadFile(bucket: Bucket, path: String): Future[HttpResponse]
  def listBuckets(): Future[Seq[Bucket]]
  def createBucket(name: String): Future[Unit]
  def getBucket(name: String): Future[Option[Bucket]]
  def listFiles(bucket: Bucket): Future[Seq[Resource]]
  def createDynamicLargeObjectManifest(bucket: Bucket, path: String, storagePath: String): Future[Unit]
  def sendFile(bucket: Bucket, path: String, contentType: String, content: Array[Byte]): Future[Unit]
  def sendFile(bucket: Bucket, path: String, contentType: String, content: InputStream): Future[Unit] = {
    val out = new ByteArrayOutputStream()
    val bufferSize = 2048
    val buffer = Array.ofDim[Byte](bufferSize)
    var readBytes: Int = -1
    while ({
      readBytes = content.read(buffer)
      readBytes != -1
    }) {
      out.write(buffer, 0, readBytes)
    }
    sendFile(bucket, path, contentType, out.toByteArray)
  }
  def sendFile(bucket: Bucket, path: String, contentType: String, content: File): Future[Unit] = {
    sendFile(bucket, path, contentType, new FileInputStream(content))
  }

}

object SwiftClient {
  def create(implicit actorSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "make-openstack")): SwiftClient = {
    val configuration: Config = actorSystem.settings.config.getConfig("make-openstack")
    val keystoneVersion = configuration.getString("authentication.keystone-version")
    val baseUrl = configuration.getString("authentication.base-url")
    val tenantName = configuration.getString("authentication.tenant-name")
    val username = configuration.getString("authentication.username")
    val password = configuration.getString("authentication.password")
    val region = Some(configuration.getString("authentication.region"))

    val props = AuthenticationActorProps(
      protocol = keystoneVersion,
      baseUrl = baseUrl,
      username = username,
      password = password,
      tenantName = tenantName,
      region = region
    )
    val initialBuckets: Seq[String] = {
      val initContainers = configuration.getStringList("storage.init-containers")
      initContainers.toArray(Array.ofDim[String](initContainers.size())).toSeq
    }

    new ActorBasedSwiftClient(props, initialBuckets)
  }

  final case class `X-Auth-Token`(override val value: String) extends ModeledCustomHeader[`X-Auth-Token`] {
    override def companion: ModeledCustomHeaderCompanion[`X-Auth-Token`] = `X-Auth-Token`
    override def renderInRequests: Boolean = true
    override def renderInResponses: Boolean = true
  }

  object `X-Auth-Token` extends ModeledCustomHeaderCompanion[`X-Auth-Token`] {
    override val name: String = "X-Auth-Token"
    override def parse(value: String): Try[`X-Auth-Token`] = Success(new `X-Auth-Token`(value))
  }

  final case class `X-Object-Manifest`(override val value: String) extends ModeledCustomHeader[`X-Object-Manifest`] {
    override def companion: ModeledCustomHeaderCompanion[`X-Object-Manifest`] = `X-Object-Manifest`
    override def renderInRequests: Boolean = true
    override def renderInResponses: Boolean = true
  }

  object `X-Object-Manifest` extends ModeledCustomHeaderCompanion[`X-Object-Manifest`] {
    override val name: String = "X-Object-Manifest"
    override def parse(value: String): Try[`X-Object-Manifest`] = Success(new `X-Object-Manifest`(value))
  }

}
