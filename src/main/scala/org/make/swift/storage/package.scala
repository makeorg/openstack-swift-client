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

package org.make.swift

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import io.circe.Decoder
import io.circe.parser.parse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

package object storage {

  implicit class RichFutureHttpResponse(val self: Future[HttpResponse]) {

    def decodeAs[T](implicit decoder: Decoder[T], actorSystem: ActorSystem[_]): Future[T] = {
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
