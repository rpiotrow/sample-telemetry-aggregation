package com.siili.aggregation.processing.consumer

import java.time.Instant

import com.siili.aggregation.persistance.Aggregation
import com.siili.aggregation.processing.{SignalValues, VehicleSignalsSample}
import zio.stm.TMap
import zio.test._
import zio.test.Assertion._

object ConsumerSpec extends DefaultRunnableSpec {

  val initialConsumer = (for {
    tMap <- TMap.empty[String, Aggregation]
  } yield new SampleConsumer(tMap)).commit

  def spec = suite("SampleProducerSpec")(
    testM("process empty list") {
      for {
        c <- initialConsumer
        _ <- c.process(List())
        m <- c.getAggregations()
      } yield assert(m.size)(equalTo(0))
    },
    testM("process single") {
      for {
        c <- initialConsumer
        _ <- c.process(List(initialSample()))
        m <- c.getAggregations()
      } yield assert(m.size)(equalTo(1)) &&
        assert(m.keys)(equalTo(Set("1"))) &&
        assert(m.get("1").get)(
          hasField("averageSpeed", (a:Aggregation) => a.averageSpeed, equalTo(10.0f))
            && hasField("maximumSpeed", (a:Aggregation) => a.maximumSpeed, equalTo(10.0f))
            && hasField("numberOfCharges", (a:Aggregation) => a.numberOfCharges, equalTo(0L))
            && hasField("odometerFirstValue", (a:Aggregation) => a.odometerFirstValue, equalTo(BigDecimal(1001)))
        )
    },
    testM("update maximum speed") {
      val sample = initialSample()
      for {
        c <- initialConsumer
        _ <- c.process(List(sample, updateSampleForMove(sample, 15.0f)))
        a <- c.getAggregation("1").get
      } yield assert(a)(
          hasField("maximumSpeed", (a:Aggregation) => a.maximumSpeed, equalTo(15.0f))
        )
    },
    testM("update average speed") {
      val sample = initialSample()
      for {
        c <- initialConsumer
        _ <- c.process(List(sample, updateSampleForMove(sample, 20.0f)))
        a <- c.getAggregation("1").get
      } yield assert(a)(
        hasField("averageSpeed", (a:Aggregation) => a.averageSpeed, equalTo(72.0f))
      )
    },
    testM("update number of charges") {
      val sample = initialSample()
      for {
        c <- initialConsumer
        _ <- c.process(List(sample, updateSampleForCharge(sample)))
        a <- c.getAggregation("1").get
      } yield assert(a)(
        hasField("numberOfCharges", (a:Aggregation) => a.numberOfCharges, equalTo(1L))
      )
    },
    testM("do not update number of charges on charging continuation") {
      val sample = initialSample()
      val charging = updateSampleForCharge(sample)
      val stillCharging = updateSampleForCharge(sample)
      for {
        c <- initialConsumer
        _ <- c.process(List(sample, charging, stillCharging))
        a <- c.getAggregation("1").get
      } yield assert(a)(
        hasField("numberOfCharges", (a:Aggregation) => a.numberOfCharges, equalTo(1L))
      )
    },
  )

  private def initialSample(vehicleId: String = "1") = VehicleSignalsSample(
    vehicleId = vehicleId,
    recordedAt = Instant.now(),
    signalValues = SignalValues(
      currentSpeed = 10.0f,
      odometer = BigDecimal(1001),
      uptime = 0,
      isCharging = false
    )
  )

  private def updateSampleForMove(
    sample: VehicleSignalsSample,
    newSpeed: Float,
    distance: BigDecimal = BigDecimal(0.01d),
    timeDiff: Long = 500
  ): VehicleSignalsSample =
    sample.copy(
      recordedAt = Instant.ofEpochMilli(sample.recordedAt.toEpochMilli + timeDiff),
      signalValues = sample.signalValues.copy(
        currentSpeed = newSpeed,
        odometer = sample.signalValues.odometer + distance,
        uptime = sample.signalValues.uptime + timeDiff
      )
    )

  private def updateSampleForCharge(
    sample: VehicleSignalsSample,
    timeDiff: Long = 500
  ): VehicleSignalsSample =
    sample.copy(
      recordedAt = Instant.ofEpochMilli(sample.recordedAt.toEpochMilli + timeDiff),
      signalValues = sample.signalValues.copy(
        currentSpeed = 0,
        uptime = sample.signalValues.uptime + timeDiff,
        isCharging = true
      )
    )

}
