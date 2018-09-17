package org.make.swift

import java.time.ZonedDateTime

package object authentication {

  case class AuthenticationRequest(auth: AuthInfo)
  case class AuthInfo(identity: AuthIdentity, scope: Option[AuthScope])
  case class AuthIdentity(methods: Seq[String], password: AuthPassword)
  case class AuthPassword(user: AuthUser)
  case class AuthUser(name: String, domain: AuthDomain, password: String)
  case class AuthDomain(id: String, name: Option[String] = None)
  case class AuthScope(project: AuthProject)
  case class AuthProject(name: String, domain: AuthDomain, id: Option[String])

  case class AuthenticationResponse(token: AuthToken)
  case class AuthToken(audit_ids: Seq[String],
                       methods: Seq[String],
                       roles: Seq[AuthRole],
                       expires_at: ZonedDateTime,
                       project: AuthProject,
                       catalog: Seq[AuthCatalog],
                       user: AuthUserInfo,
                       issued_at: ZonedDateTime)
  case class AuthRole(id: String, name: String)
  case class AuthCatalog(endpoints: Seq[AuthEndpoint],
                         `type`: String,
                         id: String,
                         name: String)

  case class AuthEndpoint(url: String,
                          region: String,
                          interface: String,
                          id: String)

  case class AuthUserInfo(domain: AuthDomain, id: String, name: String)
}
