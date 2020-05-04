organization := "com.siili"
name := "sample-telemetry-aggregation"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-kafka" % Versions.zioKafka,
  "dev.zio" %% "zio-interop-monix" % Versions.zioMonix,

  "io.getquill" %% "quill-cassandra-monix" % Versions.quill,

  "io.circe" %% "circe-core" % Versions.circe,
  "io.circe" %% "circe-generic" % Versions.circe,
  "io.circe" %% "circe-parser" % Versions.circe,

  "org.slf4j" % "slf4j-api" % Versions.slf4j,
  "org.slf4j" % "slf4j-simple" % Versions.slf4j,

  "dev.zio" %% "zio-test"     % Versions.zio   % "test",
  "dev.zio" %% "zio-test-sbt" % Versions.zio   % "test"
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Ypartial-unification",
)
