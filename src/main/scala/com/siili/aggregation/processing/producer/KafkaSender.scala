package com.siili.aggregation.processing.producer

import com.siili.aggregation.processing.VehicleSignalsSample
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.{Schedule, ZIO, ZLayer}

object KafkaSender {
  trait Service {
    def produce[RS](numberOfVehicles: Int, schedule: Schedule[RS, Any, Any]): ZIO[Nothing, Throwable, Unit]
  }

  def produce[RS](numberOfVehicles: Int, schedule: Schedule[RS, Any, Any]): ZIO[KafkaSender, Throwable, Unit] =
    ZIO.access(_.get.produce(numberOfVehicles, schedule))

  private val kafkaProducer: ZLayer[Any, Throwable, Producer[Nothing, String, VehicleSignalsSample]] = {
    val kafkaProducerSettings: ProducerSettings = ProducerSettings(List("localhost:9093"))
    KafkaSenderService.kafkaProducer(kafkaProducerSettings)
  }

  private val kafkaSender: ZLayer[Producer[Nothing, String, VehicleSignalsSample], Throwable, KafkaSender] =
    ZLayer.fromService( kafkaProducer => new KafkaSenderService("topicName", kafkaProducer))

  val live: ZLayer[Any, Throwable, KafkaSender] = kafkaProducer >>> kafkaSender
}
