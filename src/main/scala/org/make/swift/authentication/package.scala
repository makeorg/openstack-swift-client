package org.make.swift

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.{
  ModeledCustomHeader,
  ModeledCustomHeaderCompanion
}
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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

  implicit class RichHttpFuture(val self: Future[HttpResponse]) extends AnyVal {
    def extractAuthenticationResponse: Future[AuthenticationResponse] = {
      self
        .map(response => response.headers.find(_.name() == `X-Auth-Token`.name))
        .flatMap {
          case Some(token) =>
            Future.successful(AuthenticationResponse(token.value))
          case _ =>
            Future.failed(
              new IllegalArgumentException(
                "Unable to get a valid authentication"))
        }
    }
  }
}
