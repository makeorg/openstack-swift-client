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

import akka.actor.typed.scaladsl.AskPattern.{schedulerFromActorSystem, Askable}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import akka.util.Timeout
import org.make.swift.authentication.AuthenticationActor._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object AuthenticationActor {

  private class BehaviorBuilder(props: AuthenticationActorProps)(implicit system: ActorSystem[_]) {
    val authenticator: Authenticator = Authenticator.newAuthenticator(props.protocol, props.baseUrl)

    def authenticate()(implicit context: ActorContext[AuthenticationActorProtocol]): Unit = {
      context.pipeToSelf(
        authenticator
          .authenticate(AuthenticationRequest(props.username, props.password, props.tenantName, props.region))
      ) {
        case Success(value) => AuthenticationSuccess(value)
        case Failure(e)     => AuthenticationFailure(e)
      }
    }

    def authenticating(lastAuthenticationDate: Option[ZonedDateTime], promises: Seq[Promise[StorageInformation]])(
      implicit context: ActorContext[AuthenticationActorProtocol]
    ): Behavior[AuthenticationActorProtocol] = {

      Behaviors.receiveMessage {
        case Init =>
          authenticate()
          authenticating(Some(ZonedDateTime.now()), promises)

        case AuthenticationSuccess(result) =>
          val storageInfo = StorageInformation(baseUrl = result.storageUrl, token = result.tokenInfo.token)
          promises.foreach(_.success(storageInfo))
          authenticated(result, lastAuthenticationDate)

        case AuthenticationFailure(cause) =>
          context.log.error("Unable to authenticate:", cause)
          context.scheduleOnce(10.seconds, context.self, Init)
          Behaviors.same

        case CheckTokenValidity =>
          Behaviors.same

        case GetStorageInformation(replyTo) =>
          val promise = Promise[StorageInformation]
          replyTo ! FutureStorageInformation(promise.future)
          authenticating(lastAuthenticationDate, promises :+ promise)
      }
    }

    def authenticated(authentication: AuthenticationResponse, lastAuthenticationDate: Option[ZonedDateTime])(
      implicit context: ActorContext[AuthenticationActorProtocol]
    ): Behavior[AuthenticationActorProtocol] = {
      Behaviors.receiveMessage {
        case Init =>
          Behaviors.same
        case GetStorageInformation(replyTo) =>
          val information =
            StorageInformation(baseUrl = authentication.storageUrl, token = authentication.tokenInfo.token)
          replyTo ! FutureStorageInformation(Future.successful(information))
          Behaviors.same
        case CheckTokenValidity =>
          val now = ZonedDateTime.now()

          // Try to re-authenticate if token will expire in less than 10 minutes,
          // and leave some time between 2 authentication tries
          if (now.until(authentication.tokenInfo.expiresAt, ChronoUnit.MINUTES) <= 10 &&
              lastAuthenticationDate.forall(_.until(now, ChronoUnit.SECONDS) >= 10)) {
            authenticate()
            authenticated(authentication, Some(ZonedDateTime.now()))
          } else {
            Behaviors.same
          }
        case AuthenticationFailure(cause) =>
          context.log.error("Unable to authenticate:", cause)
          context.scheduleOnce(10.seconds, context.self, CheckTokenValidity)
          Behaviors.same
        case AuthenticationSuccess(result) =>
          authenticated(result, lastAuthenticationDate)
      }
    }

    def build(): Behavior[AuthenticationActorProtocol] = {
      Behaviors.setup { implicit context =>
        context.self ! Init
        Behaviors.withTimers { timers =>
          timers.startTimerAtFixedRate(CheckTokenValidity, 5.minutes)
          authenticating(None, Seq.empty)
        }
      }
    }
  }

  def createBehavior(
    props: AuthenticationActorProps
  )(implicit system: ActorSystem[_]): Behavior[AuthenticationActorProtocol] = {
    new BehaviorBuilder(props).build()
  }

  case class AuthenticationActorProps(
    protocol: String,
    baseUrl: String,
    username: String,
    password: String,
    tenantName: String,
    region: Option[String]
  )

  sealed trait AuthenticationActorProtocol
  case object Init extends AuthenticationActorProtocol
  case class GetStorageInformation(replyTo: ActorRef[FutureStorageInformation]) extends AuthenticationActorProtocol
  case class AuthenticationSuccess(result: AuthenticationResponse) extends AuthenticationActorProtocol
  case class AuthenticationFailure(cause: Throwable) extends AuthenticationActorProtocol
  case object CheckTokenValidity extends AuthenticationActorProtocol

  sealed trait StorageResponse
  case class StorageInformation(token: String, baseUrl: String) extends StorageResponse
  case class FutureStorageInformation(information: Future[StorageInformation]) extends StorageResponse

  case object NotAuthenticated

}

class AuthenticationActorService(reference: ActorRef[AuthenticationActorProtocol]) {

  def getStorageInformation()(
    implicit timeout: Timeout,
    system: ActorSystem[_],
    executionContext: ExecutionContext
  ): Future[StorageInformation] = {
    reference
      .ask(GetStorageInformation)
      .flatMap(_.information)
  }
}
