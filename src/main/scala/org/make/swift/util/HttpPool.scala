package org.make.swift.util

import java.net.URL

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{ActorAttributes, ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

abstract class HttpPool(baseUrl: String)(implicit actorSystem: ActorSystem) {

  lazy val pool: Flow[(HttpRequest, Promise[HttpResponse]),
                      (Try[HttpResponse], Promise[HttpResponse]),
                      Http.HostConnectionPool] = {
    val url = new URL(baseUrl)
    if (url.getProtocol.toLowerCase.contains("https")) {
      Http(actorSystem).cachedHostConnectionPoolHttps[Promise[HttpResponse]](host = url.getHost, port = url.getPort)
    } else {
      Http(actorSystem).cachedHostConnectionPool[Promise[HttpResponse]](host = url.getHost, port = url.getPort)
    }
  }

  lazy val queue: SourceQueueWithComplete[(HttpRequest, Promise[HttpResponse])] = Source
    .queue[(HttpRequest, Promise[HttpResponse])](bufferSize = 50, OverflowStrategy.backpressure)
    .via(pool)
    .withAttributes(ActorAttributes.dispatcher("make-openstack.dispatcher"))
    .toMat(Sink.foreach {
      case (Success(resp), p) => p.success(resp)
      case (Failure(e), p)    => p.failure(e)
    })(Keep.left)
    .run()(ActorMaterializer()(actorSystem))

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
