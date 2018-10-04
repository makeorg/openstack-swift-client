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

import java.time.ZonedDateTime

import scala.concurrent.Future

class KeystoneV3Authenticator(baseUrl: String) extends Authenticator {

  override def authenticate(request: AuthenticationRequest): Future[AuthenticationResponse] =
    ???
}

object KeystoneV3Authenticator {

  final case class AuthenticationRequest(auth: AuthInfo)

  final case class AuthInfo(identity: AuthIdentity, scope: Option[AuthScope])

  final case class AuthIdentity(methods: Seq[String], password: AuthPassword)

  final case class AuthPassword(user: AuthUser)

  final case class AuthUser(name: String, domain: AuthDomain, password: String)

  final case class AuthDomain(id: String, name: Option[String] = None)

  final case class AuthScope(project: AuthProject)

  final case class AuthProject(name: String, domain: AuthDomain, id: Option[String])

  final case class AuthenticationResponse(token: AuthToken)

  final case class AuthToken(audit_ids: Seq[String],
                             methods: Seq[String],
                             roles: Seq[AuthRole],
                             expires_at: ZonedDateTime,
                             project: AuthProject,
                             catalog: Seq[AuthCatalog],
                             user: AuthUserInfo,
                             issued_at: ZonedDateTime)

  final case class AuthRole(id: String, name: String)

  final case class AuthCatalog(endpoints: Seq[AuthEndpoint], `type`: String, id: String, name: String)

  final case class AuthEndpoint(url: String, region: String, interface: String, id: String)

  final case class AuthUserInfo(domain: AuthDomain, id: String, name: String)

}
