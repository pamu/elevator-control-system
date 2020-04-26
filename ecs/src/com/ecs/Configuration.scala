package com.ecs

import pureconfig._
import pureconfig.generic.auto._
import zio.duration.Duration

/**
 * Configuration
 * @param totalFloors             Total floors elevators are serving.
 * @param totalElevators          Total elevator count.
 * @param oneFloorTravelDuration  Time taken to travel one floor (Used in simulation).
 * @param waitingDuration         Waiting duration once elevator has stopped for people to get-out or get-in.
 */
case class Configuration(
    totalFloors: Int,
    totalElevators: Int,
    oneFloorTravelDuration: Duration,
    waitingDuration: Duration
)

object Configuration {

  implicit val zioDurationConfigReader: ConfigReader[Duration] =
    ConfigReader[scala.concurrent.duration.Duration].map(Duration.fromScala)

  lazy val config: Configuration =
    ConfigSource.default.loadOrThrow[Configuration]
}
