package org.make.swift.authentication

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{
  ModeledCustomHeader,
  ModeledCustomHeaderCompanion
}
import akka.http.scaladsl.model.{HttpRequest, Uri}
import org.make.swift.authentication.KeystoneV1Authenticator.{
  `X-Storage-Pass`,
  `X-Storage-User`
}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.{Success, Try}

class KeystoneV1Authenticator(baseUrl: String) extends Authenticator {

  override def authenticate(login: String,
                            password: String,
                            tenant: String): Future[AuthenticationResponse] = {

    Http()
      .singleRequest(
        HttpRequest(
          uri = Uri(baseUrl),
          headers =
            Seq(`X-Storage-User`(s"$tenant:$login"), `X-Storage-Pass`(password))
        )
      )
      .map { response =>
        println(response)
        response
      }
      .extractAuthenticationResponse

  }
}

object KeystoneV1Authenticator {
  final case class `X-Storage-User`(override val value: String)
      extends ModeledCustomHeader[`X-Storage-User`] {
    override def companion: ModeledCustomHeaderCompanion[`X-Storage-User`] =
      `X-Storage-User`
    override def renderInRequests: Boolean = true
    override def renderInResponses: Boolean = true
  }

  object `X-Storage-User`
      extends ModeledCustomHeaderCompanion[`X-Storage-User`] {
    override val name: String = "X-Storage-User"
    override def parse(value: String): Try[`X-Storage-User`] =
      Success(new `X-Storage-User`(value))
  }

  final case class `X-Storage-Pass`(override val value: String)
      extends ModeledCustomHeader[`X-Storage-Pass`] {
    override def companion: ModeledCustomHeaderCompanion[`X-Storage-Pass`] =
      `X-Storage-Pass`
    override def renderInRequests: Boolean = true
    override def renderInResponses: Boolean = true
  }

  object `X-Storage-Pass`
      extends ModeledCustomHeaderCompanion[`X-Storage-Pass`] {
    override val name: String = "X-Storage-Pass"
    override def parse(value: String): Try[`X-Storage-Pass`] =
      Success(new `X-Storage-Pass`(value))
  }

}
