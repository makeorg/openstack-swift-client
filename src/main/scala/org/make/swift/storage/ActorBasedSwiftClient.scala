package org.make.swift.storage

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.make.swift.SwiftClient
import org.make.swift.authentication.AuthenticationActor.AuthenticationActorProps
import org.make.swift.authentication.{AuthenticationActor, AuthenticationActorService}
import org.make.swift.model.{Bucket, Resource}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class ActorBasedSwiftClient(actorSystem: ActorSystem,
                            authenticationProps: AuthenticationActorProps,
                            initialBuckets: Seq[String])
    extends SwiftClient {

  private val actor: ActorRef = {
    val props = AuthenticationActor.props(authenticationProps)
    actorSystem.asInstanceOf[ExtendedActorSystem].systemActorOf(props, "swift-authenticator")
  }

  private implicit val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)

  private val authService = new AuthenticationActorService(actor)

  // Timeout is low, since authenticator should reply right away
  implicit val timeout: Timeout = Timeout(3.seconds)

  override def listBuckets(): Future[Seq[Bucket]] = {
    authService
      .getStorageInformation()
      .flatMap { information =>
        Http(actorSystem).singleRequest(HttpRequest(uri = s"${information.baseUrl}?format=json"))
      }
      .decodeAs[Seq[Bucket]]
  }

  override def getBucket(name: String): Future[Option[Bucket]] = {
    authService
      .getStorageInformation()
      .flatMap { information =>
        Http(actorSystem).singleRequest(HttpRequest(uri = s"${information.baseUrl}?format=json"))
      }
      .decodeAs[Seq[Bucket]]
      .map(_.find(_.name == name))
  }

  override def listFiles(bucket: Bucket): Future[Seq[Resource]] = {
    authService
      .getStorageInformation()
      .flatMap { information =>
        Http(actorSystem).singleRequest(HttpRequest(uri = s"${information.baseUrl}/${bucket.name}?format=json"))
      }
      .decodeAs[Seq[Resource]]
  }

  override def sendFile(bucket: Bucket, path: String, contentType: String, content: Array[Byte]): Future[Unit] = {
    ContentType.parse(contentType) match {
      case Left(_) => Future.failed(new IllegalArgumentException(s"Invalid content-type $contentType"))
      case Right(parsedContentType) =>
        authService
          .getStorageInformation()
          .flatMap { information =>
            Http(actorSystem).singleRequest(
              HttpRequest(
                uri = s"${information.baseUrl}/${bucket.name}/$path",
                method = HttpMethods.PUT,
                entity = HttpEntity(parsedContentType, content)
              )
            )
          }
          .flatMap { response =>
            if (response.status.isSuccess()) {
              Future.successful {}
            } else {
              Future.failed(
                new IllegalStateException(s"Sending file failed with code ${response.status}: ${response.entity}")
              )
            }
          }
    }
  }

  override def createBucket(name: String): Future[Unit] = {
    authService
      .getStorageInformation()
      .flatMap { information =>
        Http(actorSystem).singleRequest(HttpRequest(uri = s"${information.baseUrl}/$name", method = HttpMethods.PUT))
      }
      .flatMap { response =>
        if (response.status.isSuccess()) {
          Future.successful {}
        } else {
          Future.failed(new IllegalArgumentException(s"Bucket creation has failed: ${response.entity.toString}"))
        }
      }
  }

  override def init(): Future[Unit] = {
    listBuckets().map { buckets =>
      initialBuckets.filter { name =>
        !buckets.exists(_.name == name)
      }
    }.flatMap { missingBuckets =>
      Future.traverse(missingBuckets)(createBucket)
    }.map(_ => ())
  }
}
