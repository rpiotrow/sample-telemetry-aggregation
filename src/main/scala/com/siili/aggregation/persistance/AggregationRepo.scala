package com.siili.aggregation.persistance

import zio.{Layer, RIO, Task, ZLayer}

object AggregationRepo {

  trait Service {
    def read(vehicleId: String): Task[Option[Aggregation]]
    def update(aggregation: Aggregation): Task[Unit]
  }

  val live: Layer[Throwable, AggregationRepo] =
    ZLayer.fromManaged(AggregationCassandraRepo.create())

  def read(vehicleId: String): RIO[AggregationRepo, Option[Aggregation]] = RIO.accessM(_.get.read(vehicleId))
  def update(aggregation: Aggregation): RIO[AggregationRepo, Unit] = RIO.accessM(_.get.update(aggregation))

}
