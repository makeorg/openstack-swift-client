package org.make.swift.authentication

import java.time.ZonedDateTime

import akka.actor.ActorSystem

import scala.concurrent.Future

trait Authenticator {

  def authenticate(authenticationRequest: AuthenticationRequest): Future[AuthenticationResponse]
}

final case class AuthenticationRequest(login: String,
                                       password: String,
                                       tenantName: String,
                                       region: Option[String] = None)

final case class AuthenticationResponse(tokenInfo: TokenInfo, storageUrl: String)

final case class TokenInfo(token: String, issuedAt: ZonedDateTime, expiresAt: ZonedDateTime)

object Authenticator {

  val KeystoneV1 = "keystone-V1"
  val KeystoneV2 = "keystone-V2"
  val KeystoneV3 = "keystone-V3"

  def newAuthenticator(protocol: String, baseUrl: String)(implicit actorSystem: ActorSystem): Authenticator = {
    protocol match {
      case `KeystoneV1` => new KeystoneV1Authenticator(baseUrl)
      case `KeystoneV2` => new KeystoneV2Authenticator(baseUrl)
      case `KeystoneV3` => new KeystoneV3Authenticator(baseUrl)
      case other =>
        throw new IllegalArgumentException(
          s"'$other' is not a valid authentication version, supported versions are '$KeystoneV1' and '$KeystoneV2'"
        )
    }
  }

}
