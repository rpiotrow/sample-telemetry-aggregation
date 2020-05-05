package com.siili.aggregation.processing.consumer

import java.util.Date

import com.siili.aggregation.persistance.{Aggregation, AggregationRepo}
import com.siili.aggregation.processing.VehicleSignalsSample
import zio.console.{Console, putStrLn}
import zio._
import zio.stm.TMap

class SampleConsumer(
  private val tMap: TMap[String, Aggregation],
  private val aggregationRepo: AggregationRepo.Service
) {

  def process(batch: List[VehicleSignalsSample]): ZIO[Console, Throwable, Unit] = {
    val taskList = batch.map { sample =>
      for {
        aggregation <- aggregate(sample.vehicleId, sample)
        _ <- tMap.put(sample.vehicleId, aggregation).commit
      } yield sample.vehicleId
    }
    ZIO.collectAll(taskList).flatMap { ids =>
      ZIO.collectAll(ids.toSet.map { id: String => writeAggregation(id) })
    }.map(_ => ())
  }

  def getAggregations() = tMap.toMap.commit
  def getAggregation(vehicleId: String) = tMap.get(vehicleId).commit

  private def aggregate(vehicleId: String, sample: VehicleSignalsSample): ZIO[Any, Throwable, Aggregation] =
    for {
      fromMemory <- tMap.get(vehicleId).commit
      fromMemoryOrDb <- fromMemory.fold(aggregationRepo.read(vehicleId))(a => ZIO.succeed(Some(a)))
      aggregation = fromMemoryOrDb.fold(initialAggregation(sample))(a => processSample(a, sample))
    } yield aggregation

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

  private def writeAggregation(vehicleId: String): ZIO[Console, Throwable, Unit] =
    for {
      v <- tMap.get(vehicleId).commit
      _ <- v.fold(noAggregationError(vehicleId))(writeAggregation)
    } yield ()

  private def noAggregationError(vehicleId: String): ZIO[Console, Throwable, Unit] = {
    ZIO.fail(new RuntimeException(s"there is no aggregation for $vehicleId in the memory!"))
  }

  private def writeAggregation(a: Aggregation): ZIO[Console, Throwable, Unit] = {
    aggregationRepo.update(a) *>
      putStrLn(s"aggregation for ${a.vehicleId} stored to Cassandra")
  }

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
