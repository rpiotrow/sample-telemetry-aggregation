package com.siili.aggregation

import com.siili.aggregation.persistance.AggregationRepo
import zio._
import zio.console._

object Main extends App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val app = for {
      a <- AggregationRepo.read("1")
      _ <- a.fold(putStrLn("not found"))(a => putStrLn(a.toString()))
    } yield ()

    app.provideSomeLayer[zio.ZEnv](AggregationRepo.live).foldM(
      err => putStrLn(s"Execution failed with: $err") *> IO.succeed(1),
      _ => IO.succeed(0)
    )
  }
}
