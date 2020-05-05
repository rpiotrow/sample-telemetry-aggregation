# Aggregation service

Read sample telemetry data from Kafka and store aggregated values in Cassandra.

## Specification

Read data from Kafka with below structure:
```
case class DecodedSample(
  vehicleId: String, 
  recordedAt: LocalDateTime, 
  signalValues: SignalValues
)
case class SignalValues(
  currentSpeed: Float,
  odometer: BigDecimal,
  uptime: Long,
  isCharging: Boolean
)
```

Write to Cassandra database aggregated values for:
 * average speed
 * maximum speed
 * last message timestamp
 * number of charges

## How to run

### Run Kafka and Cassandra in docker

```shell script
$ docker-compose -f ./docker-compose.yml up -d
```

### Run application from console using sbt

```shell script
$ sbt run
```

## Used libraries
 
 * [zio](https://zio.dev/) - as runtime, for streaming  and for accessing Kafka
 * [squill](https://getquill.io/) - for accessing Cassandra database
 * [circe](https://circe.github.io/circe/) - for JSON serialization
