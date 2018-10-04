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
import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import org.make.swift.model.Bucket
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class StorageTest extends BaseTest with DockerSwiftAllInOne with StrictLogging {

  override def externalPort: Option[Int] = Some(StorageTest.port)

  implicit val system: ActorSystem = ActorSystem("tests", StorageTest.configuration)
  val swiftClient: SwiftClient = SwiftClient.create(system)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startAllOrFail()
    Await.result(swiftClient.init(), atMost = 30.seconds)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopAllQuietly()
    system.terminate()
  }

  feature("buckets") {
    scenario("initial buckets") {
      whenReady(swiftClient.getBucket(StorageTest.initialContainer), Timeout(10.seconds)) { buckets =>
        buckets.isDefined should be(true)
        buckets.exists(_.name == StorageTest.initialContainer) should be(true)
      }
    }

    scenario("create bucket") {
      whenReady(swiftClient.createBucket("first-bucket"), Timeout(10.seconds)) { _ =>
        ()
      }

      whenReady(swiftClient.listBuckets(), Timeout(10.seconds)) { buckets =>
        buckets.size should be > 1
        buckets.exists(_.name == "first-bucket") should be(true)
      }
    }

    scenario("list-resources") {
      whenReady(swiftClient.createBucket("write-bucket"), Timeout(10.seconds)) { _ =>
        ()
      }
      var maybeBucket: Option[Bucket] = None
      whenReady(swiftClient.getBucket("write-bucket"), Timeout(10.seconds)) { result =>
        maybeBucket = result
      }

      val bucket = maybeBucket.get

      whenReady(swiftClient.listFiles(bucket), Timeout(10.seconds)) { resources =>
        resources.isEmpty should be(true)
      }

      Thread.sleep(10.seconds.toMillis)

      val bytes = """{"test": true}""".getBytes("UTF-8")
      whenReady(
        swiftClient
          .sendFile(bucket, "test.json", "application/json", bytes),
        Timeout(10.seconds)
      ) { _ =>
        ()
      }

      whenReady(swiftClient.listFiles(bucket), Timeout(10.seconds)) { resources =>
        resources.isEmpty should be(false)
        val test = resources.find(_.name == "test.json")
        test.foreach(file => logger.info(file.toString))
        test.exists(_.bytes == bytes.size) should be(true)
        test.exists(_.content_type == "application/json") should be(true)
      }

      whenReady(
        swiftClient
          .sendFile(bucket, "path1/path2/test.json", "application/json", bytes),
        Timeout(10.seconds)
      ) { _ =>
        ()
      }

      whenReady(swiftClient.listFiles(bucket), Timeout(10.seconds)) { resources =>
        resources.isEmpty should be(false)
        val test = resources.find(_.name == "path1/path2/test.json")
        test.foreach(file => logger.info(file.toString))
        test.exists(_.bytes == bytes.size) should be(true)
        test.exists(_.content_type == "application/json") should be(true)
      }

    }
  }
}

object StorageTest {
  val port: Int = 12346
  val initialContainer = "init-container"

  val configurationString: String =
    s"""
       |make-openstack {
       |  authentication {
       |    keystone-version = "keystone-V1"
       |    base-url = "http://localhost:$port/auth/v1.0"
       |    tenant-name = "test"
       |    username = "tester"
       |    password = "testing"
       |  }
       |
       |  storage {
       |    init-containers = ["$initialContainer"]
       |  }
       |}
     """.stripMargin

  def configuration: Config = ConfigFactory.load(ConfigFactory.parseString(configurationString))

}
