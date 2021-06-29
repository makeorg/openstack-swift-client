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
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import org.make.swift.model.Bucket
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class StorageTest extends BaseTest with DockerSwiftAllInOne with StrictLogging {

  override def externalPort: Option[Int] = Some(StorageTest.port)

  implicit val system: ActorSystem[_] = ActorSystem[Nothing](Behaviors.empty, "tests", StorageTest.configuration)
  val swiftClient: SwiftClient = SwiftClient.create(system)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startAllOrFail()
    Await.result(swiftClient.init(), atMost = 30.seconds)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopAllQuietly()
    close()
    system.terminate()
  }

  Feature("buckets") {
    Scenario("initial buckets") {
      whenReady(swiftClient.getBucket(StorageTest.initialContainer), Timeout(10.seconds)) { buckets =>
        buckets should be(defined)
        buckets.exists(_.name == StorageTest.initialContainer) should be(true)
      }
    }

    Scenario("create bucket") {
      whenReady(swiftClient.createBucket("first-bucket"), Timeout(10.seconds)) { _ =>
        ()
      }

      whenReady(swiftClient.listBuckets(), Timeout(10.seconds)) { buckets =>
        buckets.size should be > 1
        buckets.exists(_.name == "first-bucket") should be(true)
      }
    }

    Scenario("list-resources") {
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

      val bytes = """{"test": true}""".getBytes("UTF-8")
      whenReady(
        swiftClient
          .sendFile(bucket, "test.json", "application/json", bytes),
        Timeout(10.seconds)
      ) { _ =>
        ()
      }

      val getResource: Future[String] = swiftClient
        .downloadFile(bucket, "test.json")
        .flatMap(_.entity.toStrict(10.seconds))
        .map(_.getData().decodeString("UTF-8"))

      whenReady(getResource, Timeout(10.seconds)) { content =>
        content should be("""{"test": true}""")
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

    Scenario("dynamic large objects") {
      val bucketName = "dlo-bucket"
      whenReady(swiftClient.createBucket(bucketName), Timeout(10.seconds)) { _ =>
        ()
      }
      var maybeBucket: Option[Bucket] = None
      whenReady(swiftClient.getBucket(bucketName), Timeout(10.seconds)) { result =>
        maybeBucket = result
      }

      val bucket = maybeBucket.get

      whenReady(swiftClient.listFiles(bucket), Timeout(10.seconds)) { resources =>
        resources.isEmpty should be(true)
      }

      val bytes = """{"test1": true}""".getBytes("UTF-8")
      whenReady(
        swiftClient
          .sendFile(bucket, "dlo/test1.json", "application/json", bytes),
        Timeout(10.seconds)
      ) { _ =>
        ()
      }

      val bytes2 = """{"test2": true}""".getBytes("UTF-8")
      whenReady(
        swiftClient
          .sendFile(bucket, "dlo/test2.json", "application/json", bytes2),
        Timeout(10.seconds)
      ) { _ =>
        ()
      }

      val bytes3 = """{"test3": true}""".getBytes("UTF-8")
      whenReady(
        swiftClient
          .sendFile(bucket, "dlo/test3.json", "application/json", bytes3),
        Timeout(10.seconds)
      ) { _ =>
        ()
      }

      whenReady(
        swiftClient
          .createDynamicLargeObjectManifest(bucket, "dlo", "dlo/"),
        Timeout(10.seconds)
      ) { _ =>
        ()
      }

      val getResource: Future[String] = swiftClient
        .downloadFile(bucket, "dlo")
        .flatMap(_.entity.toStrict(10.seconds))
        .map(_.getData().decodeString("UTF-8"))

      whenReady(getResource, Timeout(10.seconds)) { content =>
        content should be("""{"test1": true}{"test2": true}{"test3": true}""")
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
