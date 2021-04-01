/*
 * Copyright 2018 Make.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.make.swift.authentication

import akka.actor.typed.ActorSystem

import java.time.ZonedDateTime
import scala.concurrent.Future

trait Authenticator {

  def authenticate(authenticationRequest: AuthenticationRequest): Future[AuthenticationResponse]
}

final case class AuthenticationRequest(
  login: String,
  password: String,
  tenantName: String,
  region: Option[String] = None
)

final case class AuthenticationResponse(tokenInfo: TokenInfo, storageUrl: String)

final case class TokenInfo(token: String, issuedAt: ZonedDateTime, expiresAt: ZonedDateTime)

object Authenticator {

  val KeystoneV1 = "keystone-V1"
  val KeystoneV2 = "keystone-V2"
  val KeystoneV3 = "keystone-V3"

  def newAuthenticator(protocol: String, baseUrl: String)(implicit actorSystem: ActorSystem[_]): Authenticator = {
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
