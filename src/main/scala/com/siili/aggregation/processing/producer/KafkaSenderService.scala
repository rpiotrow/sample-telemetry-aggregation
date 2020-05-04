package com.siili.aggregation.processing.producer

import java.util.UUID

import com.siili.aggregation.processing.VehicleSignalsSample
import com.siili.aggregation.processing.producer.KafkaSender.KafkaSenderEnv
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import zio._
import zio.blocking.Blocking
import zio.kafka.producer.Producer

class KafkaSenderService(
  private val topic: String,
  private val kafkaProducer: Producer.Service[Any, String, VehicleSignalsSample]
) extends KafkaSender.Service {

  override def produce[RS](
    numberOfVehicles: Int,
    schedule: Schedule[RS, Any, Any]
  ): ZIO[RS with KafkaSenderEnv, Throwable, Unit] = {
    new SampleProducer[Blocking, Unit]((), { (sample, _) => for {
          sent <- send(sample)
          _ <- sent
        } yield ()
    }).produce(numberOfVehicles, schedule)
  }

  private def send(sample: VehicleSignalsSample): ZIO[Blocking, Throwable, Task[RecordMetadata]] = {
    val producerRecord = new ProducerRecord(topic, UUID.randomUUID().toString, sample)
    kafkaProducer.produce(producerRecord)
  }

}
