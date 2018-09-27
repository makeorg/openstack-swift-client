package org.make.swift

import java.io.{ByteArrayInputStream, File, FileInputStream, InputStream}

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
