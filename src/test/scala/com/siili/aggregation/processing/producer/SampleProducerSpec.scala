package com.siili.aggregation.processing.producer

import com.siili.aggregation.processing.VehicleSignalsSample
import zio._
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, suite, _}

object SampleProducerSpec extends DefaultRunnableSpec {

  val sampleProducer = new SampleProducer[Any, List[VehicleSignalsSample]](
    List[VehicleSignalsSample](),
    { (sample, list) => ZIO.succeed(sample::list) }
  )

  def spec = suite("SampleProducerSpec")(
    testM("should produce 10 samples") {
      val schedule = Schedule.recurs(9)
      val result = sampleProducer.produce(2, schedule)
      assertM(result.map(_.size))(equalTo(10)) // 1 initial + 9 recurs
    }
  )

}
