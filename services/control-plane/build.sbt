ThisBuild / scalaVersion := "2.13.15"
ThisBuild / organization := "com.scala.crawler"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val akkaVersion = "2.6.21"
lazy val akkaHttpVersion = "10.2.10"

lazy val root = (project in file("."))
  .settings(
    name := "control-plane",
    Compile / mainClass := Some("com.scala.crawler.controlplane.ControlPlaneServer"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint"
    ),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.14",
      "org.flywaydb" % "flyway-core" % "10.20.1",
      "org.flywaydb" % "flyway-database-postgresql" % "10.20.1",
      "org.postgresql" % "postgresql" % "42.7.4",
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    )
  )
