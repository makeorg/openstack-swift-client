package org.make.swift

import java.io.{ByteArrayOutputStream, File, FileInputStream, InputStream}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import com.typesafe.config.Config
import org.make.swift.authentication.AuthenticationActor.AuthenticationActorProps
import org.make.swift.model.{Bucket, Resource}
import org.make.swift.storage.ActorBasedSwiftClient

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Success, Try}

trait SwiftClient {

  def init(): Future[Unit]
  def listBuckets(): Future[Seq[Bucket]]
  def createBucket(name: String): Future[Unit]
  def getBucket(name: String): Future[Option[Bucket]]
  def listFiles(bucket: Bucket): Future[Seq[Resource]]
  def sendFile(bucket: Bucket, path: String, contentType: String, content: Array[Byte]): Future[Unit]
  def sendFile(bucket: Bucket, path: String, contentType: String, content: InputStream): Future[Unit] = {
    val out = new ByteArrayOutputStream()
    val buffer = Array.ofDim[Byte](2048)
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
  def create(actorSystem: ActorSystem = ActorSystem("make-openstack")): SwiftClient = {
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
    val initialBuckets: Seq[String] = configuration.getStringList("storage.init-containers").asScala

    new ActorBasedSwiftClient(actorSystem, props, initialBuckets)
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

}
