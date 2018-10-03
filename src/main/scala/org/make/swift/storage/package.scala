package org.make.swift

import akka.http.scaladsl.model.HttpResponse
import akka.stream.ActorMaterializer
import io.circe.Decoder
import io.circe.parser.parse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

package object storage {

  implicit class RichFutureHttpResponse(val self: Future[HttpResponse]) {

    def decodeAs[T](implicit decoder: Decoder[T], materialize: ActorMaterializer): Future[T] = {
      self.flatMap { response =>
        if (response.status.isFailure()) {
          Future.failed(
            new IllegalArgumentException(s"Request failed with code ${response.status}: ${response.entity.toString}")
          )
        } else {
          response.entity
            .toStrict(5.seconds)
            .flatMap { strict =>
              parse(new String(strict.data.toArray[Byte]))
                .flatMap(_.as[T]) match {
                case Right(decoded) => Future.successful(decoded)
                case Left(e)        => Future.failed(e)
              }
            }

        }
      }
    }

  }

}
