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
import org.make.swift.util.HttpPool

import scala.concurrent.Future
import org.make.swift.authentication.KeystoneV3Authenticator.KeystoneV3AuthenticationMethod.Password
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.Uri
import org.make.swift.authentication.KeystoneV3Authenticator._
import akka.http.scaladsl.model.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.ZonedDateTime
import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationInt
import java.nio.charset.Charset

class KeystoneV3Authenticator(baseUrl: String)(implicit actorSystem: ActorSystem[_])
    extends HttpPool(baseUrl)
    with Authenticator {

  override def authenticate(authenticationRequest: AuthenticationRequest): Future[AuthenticationResponse] = {
    val entity = KeystoneV3Request(authenticationRequest)

    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(s"$baseUrl/auth/tokens"),
      headers = Vector(Accept(MediaTypes.`application/json`)),
      entity = HttpEntity(ContentTypes.`application/json`, entity.asJson.toString)
    )

    enqueue(httpRequest)
      .flatMap(extractKeystoneV3Response(authenticationRequest, _))
  }

  def extractKeystoneV3Response(
    request: AuthenticationRequest,
    httpResponse: HttpResponse
  ): Future[AuthenticationResponse] = {
    if (httpResponse.status.isFailure()) {
      httpResponse.entity.toStrict(2.second).flatMap { entity =>
        val body = entity.data.decodeString(Charset.forName("UTF-8"))
        val code = httpResponse.status.value
        Future
          .failed(new IllegalStateException(s"Swift authentication failed with code $code: $body"))
      }
    } else {
      httpResponse.entity
        .toStrict(5.seconds)
        .flatMap { strict =>
          parse(new String(strict.data.toArray[Byte]))
            .flatMap(_.as[KeystoneV3Response]) match {
            case Right(decoded) => Future.successful(decoded)
            case Left(e)        => Future.failed(e)
          }
        }
        .flatMap { decoded =>
          val maybeToken: Option[String] = httpResponse.headers.find(_.name() == "X-Subject-Token").map(_.value())
          val maybeSwift =
            decoded.token.catalog
              .find(entry => entry.`type`.toLowerCase() == "object-store" && entry.interface.forall(_ == "public"))

          val response = for {
            token  <- maybeToken
            swift  <- maybeSwift
            region <- request.region
            url    <- swift.endpoints.find(_.region == region).orElse(swift.endpoints.headOption).map(_.url)
          } yield {
            AuthenticationResponse(
              tokenInfo =
                TokenInfo(token = token, issuedAt = decoded.token.issued_at, expiresAt = decoded.token.expires_at),
              storageUrl = url
            )
          }

          response
            .map(Future.successful)
            .getOrElse(Future.failed(new IllegalArgumentException("Unable to find any matching swift service")))

        }
    }
  }

}

object KeystoneV3Authenticator {

  // Request objects
  final case class KeystoneV3Request(auth: KeystoneV3Auth)

  object KeystoneV3Request {
    implicit val encoder: Encoder[KeystoneV3Request] = deriveEncoder[KeystoneV3Request]

    def apply(request: AuthenticationRequest): KeystoneV3Request = {
      KeystoneV3Request(auth = KeystoneV3Auth(identity = KeystoneV3Identity(password =
        KeystoneV3PasswordAuthentication(user =
          KeystoneV3UserInformation(name = request.login, password = request.password, domain = KeystoneV3UserDomain())
        )
      )
      )
      )
    }
  }

  final case class KeystoneV3Auth(identity: KeystoneV3Identity)

  object KeystoneV3Auth {
    implicit val encoder: Encoder[KeystoneV3Auth] = deriveEncoder[KeystoneV3Auth]
  }

  final case class KeystoneV3Identity(
    methods: Seq[KeystoneV3AuthenticationMethod] = Seq(Password),
    password: KeystoneV3PasswordAuthentication
  )

  object KeystoneV3Identity {
    implicit val encoder: Encoder[KeystoneV3Identity] = deriveEncoder[KeystoneV3Identity]
  }

  final case class KeystoneV3PasswordAuthentication(user: KeystoneV3UserInformation)

  object KeystoneV3PasswordAuthentication {
    implicit val encoder: Encoder[KeystoneV3PasswordAuthentication] = deriveEncoder[KeystoneV3PasswordAuthentication]
  }

  final case class KeystoneV3UserInformation(name: String, password: String, domain: KeystoneV3UserDomain)

  object KeystoneV3UserInformation {
    implicit val encoder: Encoder[KeystoneV3UserInformation] = deriveEncoder[KeystoneV3UserInformation]
  }

  final case class KeystoneV3UserDomain(name: String = "Default")

  object KeystoneV3UserDomain {
    implicit val encoder: Encoder[KeystoneV3UserDomain] = deriveEncoder[KeystoneV3UserDomain]
  }

  sealed trait KeystoneV3AuthenticationMethod {
    def name: String
  }

  object KeystoneV3AuthenticationMethod {
    implicit val encoder: Encoder[KeystoneV3AuthenticationMethod] = new Encoder[KeystoneV3AuthenticationMethod] {
      def apply(method: KeystoneV3AuthenticationMethod): Json = Json.fromString(method.name)
    }

    case object Password extends KeystoneV3AuthenticationMethod {
      override val name = "password"
    }
  }

  // Response objects
  final case class KeystoneV3Response(token: KeystoneV3TokenResponse)

  object KeystoneV3Response {
    implicit val decoder: Decoder[KeystoneV3Response] = deriveDecoder[KeystoneV3Response]
  }

  final case class KeystoneV3TokenResponse(
    issued_at: ZonedDateTime,
    expires_at: ZonedDateTime,
    catalog: Seq[KeystoneV3CatalogEntry]
  )

  object KeystoneV3TokenResponse {
    implicit val decoder: Decoder[KeystoneV3TokenResponse] = deriveDecoder[KeystoneV3TokenResponse]
  }

  final case class KeystoneV3CatalogEntry(
    endpoints: Seq[KeystoneV3Service],
    `type`: String,
    name: String,
    id: String,
    interface: Option[String]
  )

  object KeystoneV3CatalogEntry {
    implicit val decoder: Decoder[KeystoneV3CatalogEntry] = deriveDecoder[KeystoneV3CatalogEntry]
  }

  final case class KeystoneV3Service(url: String, interface: String, region: String, region_id: String, id: String)

  object KeystoneV3Service {
    implicit val decoder: Decoder[KeystoneV3Service] = deriveDecoder[KeystoneV3Service]
  }
}
