package org.make.swift.authentication

import scala.concurrent.Future

trait Authenticator {

  def authenticate(login: String,
                   password: String,
                   tenant: String): Future[AuthenticationResponse]
}

case class AuthenticationResponse(token: String)

object Authenticator {

  val KeystoneV1 = "keystone-V1"
  val KeystoneV2 = "keystone-V2"
  val KeystoneV3 = "keystone-V3"

  def newAuthenticator(protocol: String, baseUrl: String): Authenticator =
    protocols(protocol)(baseUrl)

  private val protocols: Map[String, String => Authenticator] = Map(
    KeystoneV1 -> ((s: String) => new KeystoneV1Authenticator(s)),
    KeystoneV2 -> ((s: String) => new KeystoneV2Authenticator(s)),
    KeystoneV3 -> ((s: String) => new KeystoneV3Authenticator(s))
  )
}
