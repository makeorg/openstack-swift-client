package org.make.swift.model

import java.time.ZonedDateTime

import io.circe.Decoder
import io.circe.generic.semiauto

case class Bucket(count: Int, bytes: Long, name: String, last_modified: ZonedDateTime)

object Bucket {
  implicit val decoder: Decoder[Bucket] = semiauto.deriveDecoder[Bucket]
}

case class Resource(hash: String, last_modified: ZonedDateTime, bytes: Long, name: String, content_type: String)

object Resource {
  implicit val decoder: Decoder[Resource] = semiauto.deriveDecoder[Resource]
}
