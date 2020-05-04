package com.siili.aggregation.processing.consumer

import java.util.Date

import com.siili.aggregation.persistance.Aggregation
import com.siili.aggregation.processing.VehicleSignalsSample
import zio.console.Console
import zio._
import zio.stm.TMap

class SampleConsumer(private val tMap: TMap[String, Aggregation]) {

  def process(batch: List[VehicleSignalsSample]): ZIO[Console, Throwable, Unit] = {
    val taskList = batch.map { sample =>
      (for {
        maybeAggregation <- tMap.get(sample.vehicleId)
        aggregation = processSample(maybeAggregation, sample)
        _ <- tMap.put(sample.vehicleId, aggregation)
      } yield sample.vehicleId).commit
    }
    ZIO.collectAll(taskList).flatMap { ids =>
      ZIO.collectAll(ids.toSet.map(writeAggregation(_)))
    }.map(_ => ())
  }

  def getAggregations() = tMap.toMap.commit
  def getAggregation(vehicleId: String) = tMap.get(vehicleId).commit

  private def processSample(maybeAggregation: Option[Aggregation], sample: VehicleSignalsSample): Aggregation = {
    maybeAggregation
      .orElse(readAggregation(sample.vehicleId))
      .fold(initialAggregation(sample))(a => processSample(a, sample))
  }

  private def processSample(a: Aggregation, sample: VehicleSignalsSample): Aggregation = {
    val now = new Date()
    a.copy(
      lastMessage = now,
      averageSpeed = (kmDistance(a, sample) / hourUptime(a, sample)).toFloat,
      maximumSpeed = Math.max(a.maximumSpeed, sample.signalValues.currentSpeed),
      numberOfCharges = (a.isCharging, sample.signalValues.isCharging) match {
        case (false, true) => a.numberOfCharges + 1
        case _ => a.numberOfCharges
      },
      isCharging = sample.signalValues.isCharging
    )
  }

  private def kmDistance(a: Aggregation, sample: VehicleSignalsSample): BigDecimal = {
    sample.signalValues.odometer - a.odometerFirstValue
  }

  private def hourUptime(a: Aggregation, sample: VehicleSignalsSample): BigDecimal = {
    BigDecimal(sample.signalValues.uptime - a.firstUptimeValue) / BigDecimal(1000.0 * 60.0 * 60.0)
  }

  //TODO: read from AggregationRepo
  private def readAggregation(vehicleId: String): Option[Aggregation] = None

  //TODO: write using AggregationRepo
  private def writeAggregation(vehicleId: String): ZIO[Console, Throwable, Unit] =
    for {
      v <- tMap.get(vehicleId).commit
      _ <- zio.console.putStrLn(v.fold("empty")(_.toString))
    } yield ()

  private def initialAggregation(sample: VehicleSignalsSample) = {
    Aggregation(
      vehicleId = sample.vehicleId,
      averageSpeed = sample.signalValues.currentSpeed,
      maximumSpeed = sample.signalValues.currentSpeed,
      lastMessage = new Date(sample.recordedAt.toEpochMilli()),
      numberOfCharges = 0,
      isCharging = sample.signalValues.isCharging,
      firstUptimeValue = sample.signalValues.uptime,
      odometerFirstValue = sample.signalValues.odometer
    )
  }

}
