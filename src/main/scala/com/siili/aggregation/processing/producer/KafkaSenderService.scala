package com.siili.aggregation.processing.producer

import java.util.UUID

import com.siili.aggregation.processing.VehicleSignalsSample
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.errors.SerializationException
import zio.kafka.producer.{Producer, _}
import zio.kafka.serde._
import zio.{Schedule, Task, ZIO, _}

class KafkaSenderService(topic: String, kafkaProducer: Producer.Service[Nothing, String, VehicleSignalsSample]) extends KafkaSender.Service {

  def produce[RS](numberOfVehicles: Int, schedule: Schedule[RS, Any, Any]): ZIO[Nothing, Throwable, Unit] = {
    new SampleProducer[Nothing, Unit]((), { (sample, _) => for {
          sent <- send(sample)
          _ <- sent
        } yield ()
    }).produce(numberOfVehicles, schedule)
  }

  private def send(sample: VehicleSignalsSample): ZIO[Nothing, Throwable, Task[RecordMetadata]] = {
    val producerRecord = new ProducerRecord(topic, UUID.randomUUID().toString, sample)
    kafkaProducer.produce(producerRecord)
  }

}

object KafkaSenderService {
  private val vehicleSignalsSampleSerde: Serde[Any, VehicleSignalsSample] = Serde.string.inmap {
    decode[VehicleSignalsSample](_) match {
      case Left(err)    => throw new SerializationException(err)
      case Right(value) => value
    }
  } (_.asJson.noSpaces)

  def kafkaProducer(producerSettings: ProducerSettings): ZLayer[Any, Throwable, Producer[Nothing, String, VehicleSignalsSample]] =
    Producer.make[Nothing, String, VehicleSignalsSample](producerSettings, Serde.string, vehicleSignalsSampleSerde)
}
