package com.siili.aggregation.processing.producer

import java.lang.Math.{max, min}
import java.time.{Duration, Instant}
import java.util.UUID

import com.siili.aggregation.processing.{SignalValues, VehicleSignalsSample}
import zio._
import zio.console.Console
import zio.random.Random
import zio.stream._

class SampleProducer[R, A](
  private val initial: A,
  private val effect: (VehicleSignalsSample, A) => ZIO[R, Throwable, A]
) {

  def produce[RS](
    numberOfVehicles: Int,
    schedule: Schedule[RS, Any, Any]
  ): ZIO[R with RS with Random with Console, Throwable, A] = {
    for {
      initialVehicles <- newVehicles(numberOfVehicles)
      a <- ZStream.fromSchedule(schedule).foldM((initialVehicles, initial)) { (t, _) =>
        val (vehicles, state) = t
        for {
          index <- random.nextInt(numberOfVehicles)
          vehicle <- moveVehicle(vehicles(index))
          newState <- effect(sample(vehicle), state)
        } yield (vehicles.updated(index, vehicle), newState)
      }
    } yield a._2
  }

  private case class SimulatedVehicle(
     vehicleId: String,
     created: Instant,
     odometer: BigDecimal,
     speed: Float,
     isCharging: Boolean,
     lastChange: Instant
   ) {
    override def toString: String = {
      s"SimulatedVehicle(id=$vehicleId, odometer=$odometer, speed=$speed, charging=$isCharging)"
    }
  }

  private def newVehicle(): ZIO[Random, Throwable, SimulatedVehicle] = {
    for {
      id <- ZIO.effect(UUID.randomUUID().toString)
      speed <- random.nextInt(70)
    } yield SimulatedVehicle(
        vehicleId = id,
        created = Instant.now(),
        odometer = 0,
        speed = speed,
        isCharging = false,
        lastChange = Instant.now()
      )
  }

  private def newVehicles(numberOfVehicles: Int): ZIO[Random, Throwable, Vector[SimulatedVehicle]] = {
    val list = List.range(0, numberOfVehicles).map { _ => newVehicle() }
    ZIO.collectAll(list).map(_.toVector)
  }

  private def moveVehicle(vehicle: SimulatedVehicle): ZIO[Random, Throwable, SimulatedVehicle] = {
    for {
      charging <- random.nextInt(100).map { r => if (r < 5) !vehicle.isCharging else vehicle.isCharging }
      speedDifference <- random.nextInt(20)
      newSpeed <- if (charging) ZIO.succeed(0.0f) else newRandomSpeed(vehicle.speed, speedDifference)
      distance = calculateMovedDistance(vehicle)
    } yield vehicle.copy(
      speed = newSpeed,
      odometer = vehicle.odometer + distance,
      isCharging = charging,
      lastChange = Instant.now()
    )
  }

  private def newRandomSpeed(currentSpeed: Float, speedDifference: Int) = {
    random.nextBoolean.map { accelerate =>
      max(0.0f, min(140.0f, if (accelerate) currentSpeed + speedDifference else currentSpeed - speedDifference))
    }
  }

  private def calculateMovedDistance(vehicle: SimulatedVehicle): BigDecimal = {
    if (vehicle.speed.toDouble > 0.0d) {
      val timeInHours = BigDecimal(Duration.between(vehicle.lastChange, Instant.now()).toMillis) / BigDecimal(1000 * 60 * 60)
      val distanceInKm = BigDecimal(vehicle.speed.toDouble) * timeInHours
      distanceInKm
    } else {
      0
    }
  }

  private def sample(vehicle: SimulatedVehicle) = VehicleSignalsSample(
    vehicleId = vehicle.vehicleId,
    recordedAt = Instant.now(),
    signalValues = SignalValues(
      currentSpeed = vehicle.speed,
      odometer = vehicle.odometer,
      uptime = Instant.now().toEpochMilli - vehicle.created.toEpochMilli,
      isCharging = vehicle.isCharging
    )
  )

}
