package org.make.swift.authentication

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{
  Date,
  ModeledCustomHeader,
  ModeledCustomHeaderCompanion
}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import org.make.swift.authentication.KeystoneV1Authenticator.{
  `X-Auth-Token-Expires`,
  `X-Storage-Pass`,
  `X-Storage-Url`,
  `X-Storage-User`
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.{Success, Try}

class KeystoneV1Authenticator(baseUrl: String) extends Authenticator {

  override def authenticate(
      request: AuthenticationRequest): Future[AuthenticationResponse] = {

    Http()
      .singleRequest(
        HttpRequest(
          uri = Uri(baseUrl),
          headers =
            Seq(`X-Storage-User`(s"${request.tenantName}:${request.login}"),
                `X-Storage-Pass`(request.password))
        )
      )
      .map { response =>
        println(response)
        response
      }
      .flatMap(parseResponse)

  }

  def parseResponse(response: HttpResponse): Future[AuthenticationResponse] = {
    if (response.status != StatusCodes.OK) {
      Future.failed(
        new IllegalArgumentException("Authentication did not succeed"))
    } else {
      val maybeDate = response.header[Date].map(_.date)
      val maybeExpires =
        response.headers
          .find(_.name() == `X-Auth-Token-Expires`.name)
          .map(_.value())
      val maybeToken =
        response.headers.find(_.name() == `X-Auth-Token`.name).map(_.value())
      val maybeUrl =
        response.headers.find(_.name() == `X-Storage-Url`.name).map(_.value())

      (for {
        date <- maybeDate
        expires <- maybeExpires
        token <- maybeToken
        url <- maybeUrl
      } yield {
        Future.successful(
          AuthenticationResponse(
            storageUrl = url,
            tokenInfo =
              TokenInfo(token,
                        date.toZonedDateTime,
                        (date + expires.toLong * 1000).toZonedDateTime)
          ))
      }).getOrElse(Future.failed(new IllegalArgumentException(
        "Unable to parse authentication response")))

    }
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

  final case class `X-Storage-Url`(override val value: String)
      extends ModeledCustomHeader[`X-Storage-Url`] {
    override def companion: ModeledCustomHeaderCompanion[`X-Storage-Url`] =
      `X-Storage-Url`
    override def renderInRequests: Boolean = true
    override def renderInResponses: Boolean = true
  }

  object `X-Storage-Url` extends ModeledCustomHeaderCompanion[`X-Storage-Url`] {
    override val name: String = "X-Storage-Url"
    override def parse(value: String): Try[`X-Storage-Url`] =
      Success(new `X-Storage-Url`(value))
  }

  final case class `X-Auth-Token-Expires`(override val value: String)
      extends ModeledCustomHeader[`X-Auth-Token-Expires`] {
    override def companion
      : ModeledCustomHeaderCompanion[`X-Auth-Token-Expires`] =
      `X-Auth-Token-Expires`
    override def renderInRequests: Boolean = true
    override def renderInResponses: Boolean = true
  }

  object `X-Auth-Token-Expires`
      extends ModeledCustomHeaderCompanion[`X-Auth-Token-Expires`] {
    override val name: String = "X-Auth-Token-Expires"
    override def parse(value: String): Try[`X-Auth-Token-Expires`] =
      Success(new `X-Auth-Token-Expires`(value))
  }

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

}
