# openstack-swift-client

Openstack swift client in scala

## Why this client

All the openstack clients are made for the java world, with alot of "java-entreprise" components
using blocking IOs.

This client uses non-blocking IOs, using akka-http and dependencies from the scala world (mostly akka and circe)

## Using the client

In order to use it, add to your `build.sbt`

```scala
libraryDependencies += "org.make" %% "openstack-swift-client" % "1.0.3"
```

:warning: so far, this lib is compiled only for scala 2.12

To create a client:

```scala
val client = SwiftClient.create() 
``` 

if you want to re-use you actor system for it and reuse the akka-http client configuration, you can pass it to create.

:warning: the swift client will use the actor system config to be configured.

if you do not pass an actor system, make sure that the default configuration will contain all the required config keys

see [Typesafe Config](https://github.com/lightbend/config) for more information


Then, you have the following methods available:

```scala
def init(): Future[Unit]
def listBuckets(): Future[Seq[Bucket]]
def createBucket(name: String): Future[Unit]
def getBucket(name: String): Future[Option[Bucket]]
def listFiles(bucket: Bucket): Future[Seq[Resource]]
def sendFile(bucket: Bucket, path: String, contentType: String, content: Array[Byte]): Future[Unit]
def sendFile(bucket: Bucket, path: String, contentType: String, content: InputStream): Future[Unit]
def sendFile(bucket: Bucket, path: String, contentType: String, content: File): Future[Unit]
```

Where:

`init` will make sure that the required containers are created. 
If you don't need to create containers, then there is no need to call this method.


`listBuckets` will list all the containers for the configured account on the configured region

`createBucket` creates a new container

`getBucket` will retrieve a container information. This is shortcut for:
```scala
listBuckets().map(_.find(_.name == name))
```

`sendFile` will send a resource to a given container for a given path.
The path can be anything like `reports/2018/10/25/index.html` 
it cannot accept spaces, special characters haven't been tested.

The client will automatically take care of authenticating and refreshing the token when it expires.

## Swift configuration

If no actor system is defined, the configuration is loaded from the `application.conf` in the resources.

The required configuration is:

```hocon
make-openstack {
  authentication {
    keystone-version = "keystone-V2"
    base-url = "https://keystone-host/auth/v2.0"
    tenant-name = "tenant-name"
    username = "user-name"
    password = "user-password"
    region = "REG1"
  }

  storage {
    init-containers = ["my-container"]
  }
}
```

see [reference.conf](src/main/resources/reference.conf) for more information on fields.

Note: Keystone V3 is not supported yet

