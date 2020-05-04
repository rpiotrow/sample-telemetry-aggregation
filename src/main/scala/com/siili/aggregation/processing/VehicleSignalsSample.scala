package com.siili.aggregation.processing

import java.time.Instant

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
