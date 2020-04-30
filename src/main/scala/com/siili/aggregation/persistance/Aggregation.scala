package com.siili.aggregation.persistance

import java.util.Date

case class Aggregation(
  vehicleId: String,
  averageSpeed: Float,
  maximumSpeed: Float,
  lastMessage: Date,
  numberOfCharges: Long,
  firstMessage: Date,
  odometerFirstValue: Double
)
