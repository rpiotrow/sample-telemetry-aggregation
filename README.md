# Aggregation service

Read sample telemetry data from Kafka and store aggregated values in Cassandra.

## Specification

Read data from Kafka with below structure:
```
case class DecodedSample(
  vehicleId: String,
  recordedAt: Instant,
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

### Run application from the console using sbt

```shell script
$ sbt run
```

## Solution

Application contains producer of sample data and consumer.

### Producer

Producer creates samples for the specified number of vehicles according to given schedule. For each "tick"
of the schedule producer:
 * randomly chooses vehicle
 * simulates a move for chosen vehicle
 * produces a sample with data for chosen vehicle and sends sample to Kafka topic

### Consumer

Consumer reads samples from the Kafka topic. It fetches a batch of messages, process all messages from the batch
and commits offset to Kafka. Producer has aggregated values in the memory for each vehicle which sample was processed.
Aggregated values are persisted in the Cassandra database after processing of each batch (but before committing
it in Kafka).

### Assumptions

For the sake of simplicity I assumed that:
 * majority of samples is sent to Kafka in order, for samples which `recorderAt` date is older that last processed
   message (for a given vehicle) consumer updates only maximum speed, other signal values are ignored
 * current implementation of sample producer sends all samples in order (there should not be situation descibed above,
   although consumer is prepared for that)

### Known shortcomings and limitation

 * Configuration (e.g. ports and hosts to Kafka and Cassandra) is hardcoded, it should be read from file using
   [PureConfig](https://pureconfig.github.io/) or [zio-properties](https://github.com/adrianfilip/zio-properties)
 * It should be possible to run multiple consumers (forming a consumer group
   (see: https://kafka.apache.org/documentation/#intro_consumers)) to process samples of different vehicles
   in parallel (even on different hosts) - that was not tested; to ensure proper sample processing Kafka topic
   needs to be configured for partitions related to `vehicleId`
 * Consumer is much faster then producer
 * Parallel processing of samples is not fully tested with unit tests
 * JSON serialization was used (instead of Protobuf)

## Used libraries

 * [zio](https://zio.dev/) - as runtime, for streaming and for accessing Kafka
 * [squill](https://getquill.io/) - for accessing Cassandra database
 * [circe](https://circe.github.io/circe/) - for JSON serialization
