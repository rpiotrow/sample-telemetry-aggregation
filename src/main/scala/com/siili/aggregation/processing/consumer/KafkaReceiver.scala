package com.siili.aggregation.processing.consumer

import com.siili.aggregation.persistance.{Aggregation, AggregationRepo}
import org.apache.kafka.clients.consumer.ConsumerConfig
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.stm.TMap
import zio._
import zio.duration._

object KafkaReceiver {

  type KafkaReceiverEnv = Clock with Blocking with Console

  trait Service {
    def receive(): ZIO[KafkaReceiverEnv, Throwable, Unit]
  }

  def receive(): ZIO[KafkaReceiver with KafkaReceiverEnv, Throwable, Unit] =
    ZIO.accessM(_.get.receive())

  private val kafkaConsumer: ZLayer[Clock with Blocking, Throwable, Consumer] = {
    val consumerSettings = ConsumerSettings(List("localhost:9093"))
      .withGroupId("groupId")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withCloseTimeout(0.seconds)

    Consumer.make(consumerSettings)
  }

  private val sampleConsumer: ZLayer[AggregationRepo, Throwable, Has[SampleConsumer]] =
    ZLayer.fromServiceM(repo =>
      (for {
        tMap <- TMap.empty[String, Aggregation]
      } yield new SampleConsumer(tMap, repo)).commit
    )

  private val kafkaReceiver: ZLayer[Consumer with Has[SampleConsumer], Throwable, KafkaReceiver] =
    ZLayer.fromServices[Consumer.Service, SampleConsumer, KafkaReceiver.Service] { (kafkaConsumer, sampleConsumer) =>
      new KafkaReceiverService("topicName", kafkaConsumer, sampleConsumer)
    }

  val live: ZLayer[AggregationRepo with KafkaReceiverEnv, Throwable, KafkaReceiver] =
    (kafkaConsumer ++ sampleConsumer) >>> kafkaReceiver
}
