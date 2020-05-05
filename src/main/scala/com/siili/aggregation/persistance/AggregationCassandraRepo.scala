package com.siili.aggregation.persistance

import io.getquill.{CamelCase, CassandraMonixContext}
import monix.execution.Scheduler.Implicits.global
import zio.interop.monix._
import zio.{IO, Managed, ZManaged}

class AggregationCassandraRepo(ctx: CassandraMonixContext[CamelCase.type]) extends AggregationRepo.Service {

  import ctx._

  private def aggregationById(vehicleId: String) = quote {
    query[Aggregation].filter(_.vehicleId == lift(vehicleId))
  }

  override def read(vehicleId: String): zio.Task[Option[Aggregation]] = {
    IO.fromTask {
      ctx.run(aggregationById(vehicleId))
    }.map(_.headOption)
  }

  override def update(aggregation: Aggregation): zio.Task[Unit] = {
    IO.fromTask {
      ctx.run(quote {
        implicit val aggregationUpdatetMeta = updateMeta[Aggregation](_.vehicleId)
        aggregationById(aggregation.vehicleId).update(lift(aggregation))
      })
    }
  }

}

object AggregationCassandraRepo {

  def create(): Managed[Throwable, AggregationCassandraRepo] = {
    val managedCtx = ZManaged.makeEffect {
      new CassandraMonixContext(CamelCase, "cassandra")
    } (_.close())
    managedCtx.map(new AggregationCassandraRepo(_))
  }

}
