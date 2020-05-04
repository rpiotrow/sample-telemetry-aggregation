package com.siili.aggregation

import com.siili.aggregation.processing.consumer.KafkaReceiver
import com.siili.aggregation.processing.producer.{KafkaSender, SampleProducer}
import zio._
import zio.console._

object Main extends App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val race = ZIO.raceAll(
      KafkaReceiver.receive(),
      List(KafkaSender.produce(10, Schedule.forever))
    )
    race.provideSomeLayer[zio.ZEnv](KafkaSender.live ++ KafkaReceiver.live).foldM(
      err => putStrLn(s"Execution failed with: $err") *> IO.succeed(1),
      _   => putStrLn("Execution ended with success") *> IO.succeed(0)
    )
  }
}
