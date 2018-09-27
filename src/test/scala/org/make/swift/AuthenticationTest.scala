package org.make.swift

import com.typesafe.scalalogging.StrictLogging
import org.make.swift.authentication.{AuthenticationRequest, Authenticator}
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.duration.DurationInt

class AuthenticationTest extends BaseTest with DockerSwiftAllInOne with StrictLogging {

  private val port = 12345

  override def externalPort: Option[Int] = Some(port)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startAllOrFail()
  }

  feature("Swift all-in-one authentication") {

    scenario("authenticate correctly") {
      val authenticator =
        Authenticator.newAuthenticator(Authenticator.KeystoneV1, s"http://localhost:$port/auth/v1.0")

      whenReady(authenticator.authenticate(AuthenticationRequest("tester", "testing", "test")), Timeout(5.seconds)) {
        result =>
          logger.info("{}", result)
          result.tokenInfo.token.isEmpty should be(false)
      }
    }

    scenario("bad credentials") {
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
  }
}
