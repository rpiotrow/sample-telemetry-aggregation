package com.siili.aggregation.processing.consumer

import com.siili.aggregation.persistance.Aggregation
import org.apache.kafka.clients.consumer.ConsumerConfig
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.stm.TMap
import zio._
import zio.duration._

object KafkaReceiver {
  trait Service {
    def receive(): ZIO[Clock with Blocking with Console, Throwable, Unit]
  }

  def receive(): ZIO[KafkaReceiver with Console with Blocking with Clock, Throwable, Unit] =
    ZIO.accessM(_.get.receive())

  private val kafkaConsumer: ZLayer[Clock with Blocking, Throwable, Consumer] = {
    val consumerSettings = ConsumerSettings(List("localhost:9093"))
      .withGroupId("groupId")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withCloseTimeout(0.seconds)

    Consumer.make(consumerSettings)
  }

  private val sampleConsumer: Layer[Throwable, Has[SampleConsumer]] = ZLayer.fromFunctionM(_ => (for {
    tMap <- TMap.empty[String, Aggregation]
  } yield new SampleConsumer(tMap)).commit)

  private val kafkaReceiver: ZLayer[Consumer with Has[SampleConsumer], Throwable, KafkaReceiver] =
    ZLayer.fromServices[Consumer.Service, SampleConsumer, KafkaReceiver.Service] { (kafkaConsumer, sampleConsumer) =>
      new KafkaReceiverService("topicName", kafkaConsumer, sampleConsumer)
    }

  val live: ZLayer[Clock with Blocking, Throwable, KafkaReceiver] =
    (kafkaConsumer ++ sampleConsumer) >>> kafkaReceiver
}
