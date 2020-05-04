package com.siili.aggregation.persistance

import java.util.Date

case class Aggregation(
  vehicleId: String,
  averageSpeed: Float,
  maximumSpeed: Float,
  lastMessage: Date,
  numberOfCharges: Long,
  isCharging: Boolean,
  firstUptimeValue: Long,
  odometerFirstValue: BigDecimal
) {
  override def toString: String = {
    s"Aggregation(id=$vehicleId, avg=$averageSpeed, max=$maximumSpeed, charges=$numberOfCharges, charging=$isCharging)"
  }
}
