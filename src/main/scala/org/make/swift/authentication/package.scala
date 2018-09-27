package org.make.swift

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers.{
  ModeledCustomHeader,
  ModeledCustomHeaderCompanion
}
import akka.stream.ActorMaterializer

import scala.util.{Success, Try}

package object authentication {
  implicit val system: ActorSystem = ActorSystem("swift-client")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  final case class `X-Auth-Token`(override val value: String)
      extends ModeledCustomHeader[`X-Auth-Token`] {
    override def companion: ModeledCustomHeaderCompanion[`X-Auth-Token`] =
      `X-Auth-Token`

    override def renderInRequests: Boolean = true

    override def renderInResponses: Boolean = true
  }

  object `X-Auth-Token` extends ModeledCustomHeaderCompanion[`X-Auth-Token`] {
    override val name: String = "X-Auth-Token"

    override def parse(value: String): Try[`X-Auth-Token`] =
      Success(new `X-Auth-Token`(value))
  }

  implicit class RichDateTime(val self: DateTime) extends AnyVal {
    def toZonedDateTime: ZonedDateTime = ZonedDateTime.now()
  }
}
