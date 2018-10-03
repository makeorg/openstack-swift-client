package org.make.swift

import java.time.{ZoneId, ZonedDateTime}

import akka.http.scaladsl.model.DateTime

package object authentication {

  implicit class RichDateTime(val self: DateTime) extends AnyVal {
    def toZonedDateTime: ZonedDateTime = {
      ZonedDateTime.of(self.year, self.month, self.day, self.hour, self.minute, self.second, 0, ZoneId.of("GMT"))
    }
  }

}
