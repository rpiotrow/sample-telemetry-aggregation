package com.siili.aggregation.processing.producer

import com.siili.aggregation.processing.VehicleSignalsSample
import zio.blocking.Blocking
import zio.console.Console
import zio.kafka.producer._
import zio.kafka.serde.Serde
import zio.random.Random
import zio._

object KafkaSender {

  type KafkaSenderEnv = Random with Blocking with Console

  trait Service {
    def produce[RS](
      numberOfVehicles: Int,
      schedule: Schedule[RS, Any, Any]
    ): ZIO[RS with KafkaSenderEnv, Throwable, Unit]
  }

  def produce[RS](
    numberOfVehicles: Int,
    schedule: Schedule[RS, Any, Any]
  ): ZIO[KafkaSender with RS with KafkaSenderEnv, Throwable, Unit] =
    ZIO.accessM(_.get.produce(numberOfVehicles, schedule))

  private val kafkaProducer: ZLayer[Any, Throwable, Producer[Any, String, VehicleSignalsSample]] = {
    val kafkaProducerSettings: ProducerSettings = ProducerSettings(List("localhost:9093"))
    Producer.make(kafkaProducerSettings, Serde.string, VehicleSignalsSample.serde)
  }

  private val kafkaSender: ZLayer[Producer[Any, String, VehicleSignalsSample], Throwable, KafkaSender] =
    ZLayer.fromService(kafkaProducer => new KafkaSenderService("topicName", kafkaProducer))

  val live: ZLayer[Any, Throwable, KafkaSender] = kafkaProducer >>> kafkaSender
}
