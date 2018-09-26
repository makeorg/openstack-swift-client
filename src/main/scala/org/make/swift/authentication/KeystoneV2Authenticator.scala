package org.make.swift.authentication
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import io.circe.Encoder

import scala.concurrent.Future
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._
import org.make.swift.authentication.KeystoneV2Authenticator.{
  Authentication,
  AuthenticationDetails,
  AuthenticationRequest
}

// OpenStack documentation has broken links on V2 documentation
// See https://dev.cloudwatt.com/fr/doc/api/api-ref-identity-v2.html for more information if needed
class KeystoneV2Authenticator(baseUrl: String) extends Authenticator {

  override def authenticate(login: String,
                            password: String,
                            tenant: String): Future[AuthenticationResponse] = {

    val request = AuthenticationRequest(
      auth = AuthenticationDetails(
        tenantName = tenant,
        authentication = Authentication(username = login, password = password)))

    Http()
      .singleRequest(
        HttpRequest(method = HttpMethods.POST,
                    uri = Uri(s"$baseUrl/tokens"),
                    entity = HttpEntity(ContentTypes.`application/json`,
                                        request.asJson.toString))
      )
      .extractAuthenticationResponse
  }
}

object KeystoneV2Authenticator {

  final case class AuthenticationRequest(auth: AuthenticationDetails)

  object AuthenticationRequest {
    implicit val encoder: Encoder[AuthenticationRequest] =
      deriveEncoder[AuthenticationRequest]
  }

  final case class AuthenticationDetails(tenantName: String,
                                         authentication: Authentication)

  object AuthenticationDetails {
    implicit val encoder: Encoder[AuthenticationDetails] =
      deriveEncoder[AuthenticationDetails]
  }

  final case class Authentication(username: String, password: String)

  object Authentication {
    implicit val encoder: Encoder[Authentication] =
      deriveEncoder[Authentication]
  }

}
