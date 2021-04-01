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
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.make.swift.authentication.KeystoneV2Authenticator._
import org.make.swift.util.HttpPool

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

// OpenStack documentation has broken links on V2 documentation
// See https://dev.cloudwatt.com/fr/doc/api/api-ref-identity-v2.html for more information if needed
class KeystoneV2Authenticator(baseUrl: String)(implicit actorSystem: ActorSystem[_])
    extends HttpPool(baseUrl)
    with Authenticator {

  override def authenticate(request: AuthenticationRequest): Future[AuthenticationResponse] = {

    val entity = KeystoneV2AuthenticationRequest(auth = KeystoneV2AuthenticationDetails(
      tenantName = request.tenantName,
      passwordCredentials = KeystoneV2Authentication(username = request.login, password = request.password)
    )
    )

    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(s"$baseUrl/tokens"),
      headers = Seq(Accept(MediaTypes.`application/json`)),
      entity = HttpEntity(ContentTypes.`application/json`, entity.asJson.toString)
    )

    enqueue(httpRequest)
      .flatMap(extractKeystoneV2Response(request, _))
  }

  def extractKeystoneV2Response(
    request: AuthenticationRequest,
    response: HttpResponse
  ): Future[AuthenticationResponse] = {

    if (response.status != StatusCodes.OK) {
      Future.failed(new IllegalArgumentException("Connexion failed, check your credentials"))
    } else {
      response.entity
        .toStrict(5.seconds)
        .flatMap { strict =>
          parse(new String(strict.data.toArray[Byte]))
            .flatMap(_.as[KeystoneV2AuthenticationResponse]) match {
            case Right(decoded) => Future.successful(decoded)
            case Left(e)        => Future.failed(e)
          }
        }
        .flatMap { decoded =>
          val token = decoded.access.token
          val maybeSwift =
            decoded.access.serviceCatalog.find(_.name.toLowerCase() == "swift")

          val maybeUrl = (for {
            swift  <- maybeSwift
            region <- request.region
            url    <- swift.endpoints.find(_.region == region).map(_.publicURL)
          } yield url).orElse {
            maybeSwift.flatMap(_.endpoints.headOption).map(_.publicURL)
          }

          maybeUrl.map { url =>
            Future.successful(
              AuthenticationResponse(
                tokenInfo = TokenInfo(token = token.id, issuedAt = token.issued_at, expiresAt = token.expires),
                storageUrl = url
              )
            )
          }.getOrElse(Future.failed(new IllegalArgumentException("Unable to find any matching swift service")))

        }
    }

  }
}

object KeystoneV2Authenticator {

  // Keystone V2 request objects
  final case class KeystoneV2AuthenticationRequest(auth: KeystoneV2AuthenticationDetails)

  object KeystoneV2AuthenticationRequest {
    implicit val encoder: Encoder[KeystoneV2AuthenticationRequest] = deriveEncoder[KeystoneV2AuthenticationRequest]
  }

  final case class KeystoneV2AuthenticationDetails(tenantName: String, passwordCredentials: KeystoneV2Authentication)

  object KeystoneV2AuthenticationDetails {
    implicit val encoder: Encoder[KeystoneV2AuthenticationDetails] = deriveEncoder[KeystoneV2AuthenticationDetails]
  }

  final case class KeystoneV2Authentication(username: String, password: String)

  object KeystoneV2Authentication {
    implicit val encoder: Encoder[KeystoneV2Authentication] = deriveEncoder[KeystoneV2Authentication]
  }

  // Keystone V2 response objects
  final case class KeystoneV2AuthenticationResponse(access: KeystoneV2AccessInfo)

  object KeystoneV2AuthenticationResponse {
    implicit val decoder: Decoder[KeystoneV2AuthenticationResponse] = deriveDecoder[KeystoneV2AuthenticationResponse]
  }

  final case class KeystoneV2AccessInfo(
    token: KeystoneV2TokenInfo,
    serviceCatalog: Seq[KeystoneV2Service],
    user: KeystoneV2User,
    metadata: KeystoneV2Metadata
  )

  object KeystoneV2AccessInfo {
    implicit val decoder: Decoder[KeystoneV2AccessInfo] = deriveDecoder[KeystoneV2AccessInfo]
  }

  final case class KeystoneV2TokenInfo(
    issued_at: ZonedDateTime,
    expires: ZonedDateTime,
    id: String,
    audit_ids: Seq[String],
    tenant: KeystoneV2TenantInfo
  )

  object KeystoneV2TokenInfo {
    implicit val decoder: Decoder[KeystoneV2TokenInfo] = deriveDecoder[KeystoneV2TokenInfo]
  }

  final case class KeystoneV2TenantInfo(description: String, enabled: Boolean, id: String, name: String)

  object KeystoneV2TenantInfo {
    implicit val decoder: Decoder[KeystoneV2TenantInfo] = deriveDecoder[KeystoneV2TenantInfo]
  }

  final case class KeystoneV2Service(
    endpoints: Seq[KeystoneV2LocalizedService],
    endpoints_links: Seq[String],
    `type`: String,
    name: String
  )

  object KeystoneV2Service {
    implicit val decoder: Decoder[KeystoneV2Service] = deriveDecoder[KeystoneV2Service]
  }

  final case class KeystoneV2LocalizedService(
    adminURL: String,
    region: String,
    internalURL: String,
    publicURL: String,
    id: String
  )

  object KeystoneV2LocalizedService {
    implicit val decoder: Decoder[KeystoneV2LocalizedService] = deriveDecoder[KeystoneV2LocalizedService]
  }

  final case class KeystoneV2User(
    username: String,
    roles_links: Seq[String],
    id: String,
    name: String,
    roles: Seq[KeystoneV2Role]
  )

  object KeystoneV2User {
    implicit val decoder: Decoder[KeystoneV2User] = deriveDecoder[KeystoneV2User]
  }

  final case class KeystoneV2Role(name: String)

  object KeystoneV2Role {
    implicit val decoder: Decoder[KeystoneV2Role] = deriveDecoder[KeystoneV2Role]
  }

  final case class KeystoneV2Metadata(is_admin: Int, roles: Seq[String])

  object KeystoneV2Metadata {
    implicit val decoder: Decoder[KeystoneV2Metadata] = deriveDecoder[KeystoneV2Metadata]
  }

}
