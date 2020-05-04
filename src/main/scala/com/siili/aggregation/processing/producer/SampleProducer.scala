package com.siili.aggregation.processing.producer

import java.lang.Math.{max, min}
import java.time.Instant
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
          vehicle = vehicles(index)
          newState <- effect(sample(vehicle), state)
          moved <- moveVehicle(vehicle)
        } yield (vehicles.updated(index, moved), newState)
      }
    } yield a._2
  }

  private case class SimulatedVehicle(
     vehicleId: String,
     created: Instant,
     odometer: BigDecimal,
     speed: Float,
     isCharging: Boolean
   ) {
    override def toString: String = {
      s"SimulatedVehicle(id=$vehicleId, odometer=$odometer, speed=$speed, charging=$isCharging)"
    }
  }

  private def newVehicle(): ZIO[Random, Throwable, SimulatedVehicle] = {
    for {
      id <- ZIO.effect(UUID.randomUUID().toString)
      speed <- random.nextInt(70)
    } yield SimulatedVehicle(id, Instant.now(), 0, speed, false)
  }

  private def newVehicles(numberOfVehicles: Int): ZIO[Random, Throwable, Vector[SimulatedVehicle]] = {
    val list = List.range(0, numberOfVehicles).map { _ => newVehicle() }
    ZIO.collectAll(list).map(_.toVector)
  }

  private def moveVehicle(vehicle: SimulatedVehicle): ZIO[Random, Throwable, SimulatedVehicle] = {
    for {
      charging <- random.nextInt(100).map { _ < 5 }
      speedDifference <- random.nextInt(20)
      newSpeed <- if (charging) ZIO.succeed(0.0f) else newRandomSpeed(vehicle.speed, speedDifference)
      distance <- random.nextInt(1500).map { v => BigDecimal(v / 1000.0d) }
    } yield vehicle.copy(speed = newSpeed, odometer = vehicle.odometer + distance, isCharging = charging)
  }

  private def newRandomSpeed(currentSpeed: Float, speedDifference: Int) = {
    random.nextBoolean.map { accelerate =>
      max(0.0f, min(140.0f, if (accelerate) currentSpeed + speedDifference else currentSpeed - speedDifference))
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
