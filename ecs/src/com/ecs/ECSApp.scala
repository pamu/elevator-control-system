package com.ecs

import cats.data.NonEmptyList
import com.ecs.domain.{Direction, ElevatorId, Floor, Request, ValidationError}
import com.ecs.domain.Request.{DropRequest, PickupRequest}
import com.ecs.logic.{ElevatorControlSystem, ElevatorControlSystemImpl}
import zio._
import zio.console._
import zio.duration._
import zio.random._
import zio.stm.{STM, TRef}

object ECSApp extends App {

  // Print elevator state continuously forever every 1 second.
  private def printElevatorStates(ecs: ElevatorControlSystemImpl) = {
    (for {
      state <- ecs.status()
      _ <- putStrLn(
        s"Elevators: ${state.zipWithIndex.map { case (state, i) => s"E$i: $state" }.mkString("[ ", ", ", "]")}"
      )
      _ <- ZIO.sleep(1.second)
    } yield ()).forever
  }

  // Send requests for testing
  def sendRequests(ecs: ElevatorControlSystem): ZIO[Console, NonEmptyList[ValidationError], Unit] =
    (for {
      _ <- ecs.pickup(10, Direction.Up)
      _ <- ecs.drop(2, 5)
      _ <- ecs.pickup(8, Direction.Down)
    } yield ()).catchAll((errors: NonEmptyList[ValidationError]) =>
      putStrLn(s"Errors: ${errors.toList.map(_.getMessage).mkString("[", ",", "]")}")
    )

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    val config = Configuration.config
    (for {
      ecs      <- ElevatorControlSystem.make(config)
      ecsFiber <- ecs.run().fork
      _        <- sendRequests(ecs).fork
      _        <- printElevatorStates(ecs)
      _        <- ecsFiber.join
    } yield ()).as(0)
  }

}
