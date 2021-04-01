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

package org.make.swift.util

import akka.actor.typed.ActorSystem

import java.net.URL
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorAttributes, OverflowStrategy, QueueOfferResult}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

abstract class HttpPool(baseUrl: String)(implicit actorSystem: ActorSystem[_]) {

  private val defaultHttpsPort: Int = 443
  private val defaultHttpPort: Int = 80
  private val defaultBufferSize = 50

  lazy val pool: Flow[
    (HttpRequest, Promise[HttpResponse]),
    (Try[HttpResponse], Promise[HttpResponse]),
    Http.HostConnectionPool
  ] = {

    val url = new URL(baseUrl)
    if (url.getProtocol.toLowerCase.contains("https")) {
      val port = {
        val urlPort = url.getPort
        if (urlPort == -1) {
          defaultHttpsPort
        } else {
          urlPort
        }
      }
      Http(actorSystem).cachedHostConnectionPoolHttps[Promise[HttpResponse]](host = url.getHost, port = port)
    } else {
      val port = {
        val urlPort = url.getPort
        if (urlPort == -1) {
          defaultHttpPort
        } else {
          urlPort
        }
      }
      Http(actorSystem).cachedHostConnectionPool[Promise[HttpResponse]](host = url.getHost, port = port)
    }
  }

  lazy val queue: SourceQueueWithComplete[(HttpRequest, Promise[HttpResponse])] = Source
    .queue[(HttpRequest, Promise[HttpResponse])](bufferSize = defaultBufferSize, OverflowStrategy.backpressure)
    .via(pool)
    .withAttributes(ActorAttributes.dispatcher("make-openstack.dispatcher"))
    .toMat(Sink.foreach {
      case (Success(resp), p) => p.success(resp)
      case (Failure(e), p)    => p.failure(e)
    })(Keep.left)
    .run()

  def enqueue(request: HttpRequest)(implicit executionContext: ExecutionContext): Future[HttpResponse] = {
    val promise = Promise[HttpResponse]()
    queue.offer((request, promise)).flatMap {
      case QueueOfferResult.Enqueued    => promise.future
      case QueueOfferResult.Dropped     => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed =>
        Future
          .failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

}
