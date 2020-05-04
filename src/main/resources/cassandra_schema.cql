CREATE KEYSPACE IF NOT EXISTS vehicle
WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS vehicle.aggregation (
  vehicleId TEXT,
  averageSpeed FLOAT,
  maximumSpeed FLOAT,
  lastMessage TIMESTAMP,
  numberOfCharges BIGINT,
  isCharging BOOLEAN,
  firstUptimeValue BIGINT,
  odometerFirstValue DECIMAL,
  PRIMARY KEY (vehicleId)
) WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };
