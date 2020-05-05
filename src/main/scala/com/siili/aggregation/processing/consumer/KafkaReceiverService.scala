package com.siili.aggregation.processing.consumer

import com.siili.aggregation.processing.VehicleSignalsSample
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.duration._
import zio.kafka.consumer._
import zio.kafka.serde.Serde
import zio.stream.ZSink

class KafkaReceiverService(
  private val topic: String,
  private val kafkaConsumer: Consumer.Service, consumer: SampleConsumer
) extends KafkaReceiver.Service {

  override def receive(): ZIO[Clock with Blocking with Console, Throwable, Unit] = {
    kafkaConsumer
      .subscribeAnd(Subscription.topics(topic))
      .plainStream(Serde.string, VehicleSignalsSample.serde)
      .flattenChunks
      .groupedWithin(10000, 10.seconds)
      .mapM { batch =>
        putStrLn(s"batch size ${batch.size}") *> process(batch) *> commit(batch)
      }
      .run(ZSink.drain)

  }

  private def process(batch: List[CommittableRecord[String, VehicleSignalsSample]]) = {
    val samples = batch.map(_.record.value())
    consumer.process(samples)
  }

  private def commit(batch: List[CommittableRecord[String, VehicleSignalsSample]]) =
    batch.map(_.offset)
      .foldLeft(OffsetBatch.empty)(_ merge _)
      .commit

}
