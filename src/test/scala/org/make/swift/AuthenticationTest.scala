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
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.StrictLogging
import org.make.swift.authentication.{AuthenticationRequest, Authenticator}
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.duration.DurationInt

class AuthenticationTest extends BaseTest with DockerSwiftAllInOne with StrictLogging {

  private val port = 12345
  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "tests")

  override def externalPort: Option[Int] = Some(port)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startAllOrFail()
  }

  Feature("Swift all-in-one authentication") {

    Scenario("authenticate correctly") {
      val authenticator =
        Authenticator.newAuthenticator(Authenticator.KeystoneV1, s"http://localhost:$port/auth/v1.0")

      whenReady(authenticator.authenticate(AuthenticationRequest("tester", "testing", "test")), Timeout(5.seconds)) {
        result =>
          logger.info("{}", result)
          result.tokenInfo.token.isEmpty should be(false)
      }
    }

    Scenario("bad credentials") {
      val authenticator =
        Authenticator.newAuthenticator(Authenticator.KeystoneV1, s"http://localhost:$port/auth/v1.0")

      whenReady(
        authenticator
          .authenticate(AuthenticationRequest("tester", "bad-credentials", "test"))
          .failed,
        Timeout(5.seconds)
      ) { e =>
        logger.error("", e)
      }

    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopAllQuietly()
    close()
    system.terminate()
  }
}
