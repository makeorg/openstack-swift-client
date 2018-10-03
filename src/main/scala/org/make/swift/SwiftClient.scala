package org.make.swift

import java.io.{ByteArrayInputStream, File, FileInputStream, InputStream}

import com.typesafe.config.{Config, ConfigFactory}
import org.make.swift.model.{Bucket, Resource}

import scala.concurrent.Future

trait SwiftClient {

  def listBuckets(): Future[Seq[Bucket]]
  def getBucket(name: String): Future[Option[Bucket]]
  def listFiles(bucket: Bucket): Future[Seq[Resource]]
  def sendFile(bucket: Bucket, path: String, content: Array[Byte]): Future[Unit] = {
    sendFile(bucket, path, new ByteArrayInputStream(content))
  }
  def sendFile(bucket: Bucket, path: String, content: InputStream): Future[Unit]
  def sendFile(bucket: Bucket, path: String, content: File): Future[Unit] = {
    sendFile(bucket, path, new FileInputStream(content))
  }

}

object SwiftClient {
  def create(config: Config = ConfigFactory.load()): SwiftClient = {
    val keystoneVersion = config.getString("make-openstack.authentication.keystone-version")
    val baseUrl = config.getString("make-openstack.authentication.base-url")

    val tenantName = config.getString("make-openstack.authentication.tenant-name")
    val username = config.getString("make-openstack.authentication.username")
    val password = config.getString("make-openstack.authentication.password")
    val region = config.getString("make-openstack.authentication.region")

    ???
  }
}
