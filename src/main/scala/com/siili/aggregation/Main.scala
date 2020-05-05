package com.siili.aggregation

import com.siili.aggregation.persistance.AggregationRepo
import com.siili.aggregation.processing.consumer.KafkaReceiver
import com.siili.aggregation.processing.producer.KafkaSender
import zio._
import zio.console.putStrLn
import zio.duration._

object Main extends App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val app = ZIO.raceAll(
      KafkaReceiver.receive(),
      List(KafkaSender.produce(10, Schedule.spaced(10.milliseconds)))
    )
    val receiver = (AggregationRepo.live ++ ZEnv.live) >>> KafkaReceiver.live
    app.provideSomeLayer[ZEnv](KafkaSender.live ++ receiver).foldM(
      err => putStrLn(s"Execution failed with: $err") *> IO.succeed(1),
      _   => putStrLn("Execution ended with success") *> IO.succeed(0)
    )
  }
}
