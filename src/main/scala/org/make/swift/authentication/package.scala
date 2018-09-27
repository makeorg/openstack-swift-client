package org.make.swift

import java.time.{ZoneId, ZonedDateTime}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.ActorMaterializer

package object authentication {
  implicit val system: ActorSystem = ActorSystem("swift-client")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit class RichDateTime(val self: DateTime) extends AnyVal {
    def toZonedDateTime: ZonedDateTime = {
      ZonedDateTime.of(self.year, self.month, self.day, self.hour, self.minute, self.second, 0, ZoneId.of("GMT"))
    }
  }

}
