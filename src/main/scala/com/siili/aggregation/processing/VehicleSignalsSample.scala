package com.siili.aggregation.processing

import java.time.Instant

import org.apache.kafka.common.errors.SerializationException
import zio.kafka.serde.Serde

case class VehicleSignalsSample(
  vehicleId: String,
  recordedAt: Instant,
  signalValues: SignalValues
)

case class SignalValues(
  currentSpeed: Float,
  odometer: BigDecimal,
  uptime: Long,
  isCharging: Boolean
)

object VehicleSignalsSample {
  import io.circe.generic.auto._
  import io.circe.parser._
  import io.circe.syntax._

  val serde: Serde[Any, VehicleSignalsSample] = Serde.string.inmap {
    decode[VehicleSignalsSample](_) match {
      case Left(err)    => throw new SerializationException(err)
      case Right(value) => value
    }
  } (_.asJson.noSpaces)

}
