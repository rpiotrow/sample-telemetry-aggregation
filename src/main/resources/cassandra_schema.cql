CREATE KEYSPACE IF NOT EXISTS vehicle
WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS vehicle.aggregation (
  vehicleId TEXT,
  averageSpeed FLOAT,
  maximumSpeed FLOAT,
  lastMessage TIMESTAMP,
  numberOfCharges BIGINT,
  firstMessage TIMESTAMP,
  odometerFirstValue DOUBLE,
  PRIMARY KEY (vehicleId)
) WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };
