organization := "com.siili"
name := "sample-telemetry-aggregation"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-kafka" % "0.8.0",
  "dev.zio" %% "zio-interop-monix" % "3.1.0.0-RC2",

  "io.getquill" %% "quill-cassandra-monix" % "3.5.1",

  "org.slf4j" % "slf4j-api" % "1.7.30",
  "org.slf4j" % "slf4j-simple" % "1.7.30"
)

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
