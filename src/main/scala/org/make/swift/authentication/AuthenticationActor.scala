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

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import org.make.swift.authentication.AuthenticationActor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}

class AuthenticationActor(props: AuthenticationActorProps) extends Actor with StrictLogging {

  val authenticator: Authenticator = Authenticator.newAuthenticator(props.protocol, props.baseUrl)(context.system)

  var lastAuthenticationDate: Option[ZonedDateTime] = None

  override def preStart(): Unit = {
    context.become(authenticating(Seq.empty))
    self ! Init
    context.system.scheduler.schedule(5.minutes, 5.minutes, self, CheckTokenValidity)
  }

  private def authenticate(): Unit = {
    authenticator
      .authenticate(AuthenticationRequest(props.username, props.password, props.tenantName, props.region))
      .map(AuthenticationSuccess.apply)
      .recoverWith {
        case e => Future.successful(AuthenticationFailure(e))
      }
      .pipeTo(self)
    lastAuthenticationDate = Some(ZonedDateTime.now())
  }

  override def receive: Receive = {
    case _ =>
  }

  def authenticating(promises: Seq[Promise[StorageInformation]]): Receive = {
    case Init => authenticate()

    case AuthenticationSuccess(result) =>
      val storageInfo = StorageInformation(baseUrl = result.storageUrl, token = result.tokenInfo.token)
      promises.foreach(_.success(storageInfo))
      context.become(authenticated(result))

    case AuthenticationFailure(cause) =>
      logger.error("Unable to authenticate:", cause)
      context.system.scheduler.scheduleOnce(10.seconds, self, Init)

    case CheckTokenValidity =>
    case GetStorageInformation =>
      val promise = Promise[StorageInformation]
      sender() ! FutureStorageInformation(promise.future)
      context.become(authenticating(promises :+ promise))
  }

  def authenticated(authentication: AuthenticationResponse): Receive = {
    case GetStorageInformation =>
      sender() ! StorageInformation(baseUrl = authentication.storageUrl, token = authentication.tokenInfo.token)
    case CheckTokenValidity =>
      val now = ZonedDateTime.now()

      // Try to re-authenticate if token will expire in less than 10 minutes,
      // and leave some time between 2 authentication tries
      if (now.until(authentication.tokenInfo.expiresAt, ChronoUnit.MINUTES) <= 10) {
        if (lastAuthenticationDate.exists(_.until(now, ChronoUnit.SECONDS) > 10)) {
          context.system.scheduler.scheduleOnce(10.seconds, self, CheckTokenValidity)
        } else {
          authenticate()
        }
      }
    case AuthenticationFailure(cause) =>
      logger.error("Unable to authenticate:", cause)
      context.system.scheduler.scheduleOnce(10.seconds, self, CheckTokenValidity)
    case AuthenticationSuccess(result) =>
      context.become(authenticated(result))
  }
}

object AuthenticationActor {

  case class AuthenticationActorProps(protocol: String,
                                      baseUrl: String,
                                      username: String,
                                      password: String,
                                      tenantName: String,
                                      region: Option[String])

  def props(props: AuthenticationActorProps): Props = Props(new AuthenticationActor(props))

  sealed trait AuthenticationActorProtocol
  case object Init extends AuthenticationActorProtocol
  case object GetStorageInformation extends AuthenticationActorProtocol
  case class AuthenticationSuccess(result: AuthenticationResponse) extends AuthenticationActorProtocol
  case class AuthenticationFailure(cause: Throwable) extends AuthenticationActorProtocol
  case object CheckTokenValidity

  case class StorageInformation(token: String, baseUrl: String)
  case class FutureStorageInformation(information: Future[StorageInformation])

  case object NotAuthenticated

}

class AuthenticationActorService(reference: ActorRef) {

  def getStorageInformation()(implicit timeout: Timeout): Future[StorageInformation] = {
    (reference ? GetStorageInformation).flatMap {
      case info: StorageInformation             => Future.successful(info)
      case futureInfo: FutureStorageInformation => futureInfo.information
      case _                                    => Future.failed(new IllegalStateException("Unable to retrieve storage information"))
    }
  }
}
