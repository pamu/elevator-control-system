package com.ecs.logic

import cats.data.NonEmptyList
import com.ecs.Configuration
import com.ecs.domain.{Direction, Floor, Request, ValidationError, Validations}
import zio._
import zio.stm._
import cats.implicits._
import com.ecs.domain.Request.{DropRequest, PickupRequest}
import com.ecs.logic.Elevator.State.{Idle, Moving, StopOnFloor}
import com.ecs.logic.Elevator.{next, timeElapsed}
import zio.clock.Clock
import zio.console.Console

/**
  * Elevator control system interface.
  */
trait ElevatorControlSystem {

  /**
    * Fetch status of the elevators.
    * @return State of elevators.
    */
  def status(): ZIO[Any, Nothing, List[Elevator.State]]

  /**
    * Send pickup request
    * @param floor Floor from which pickup has to happen.
    * @param dir   Direction in which requester wants to go.
    * @return      Error or ACK.
    */
  def pickup(floor: Int, dir: Direction): ZIO[Any, NonEmptyList[ValidationError], Boolean]

  /**
    * Send drop request
    * @param elevatorId  Elevator id to sent drop request to.
    * @param floor       Floor at which drop is requested.
    * @return            Error or ACK.
    */
  def drop(elevatorId: Int, floor: Int): ZIO[Any, NonEmptyList[ValidationError], Boolean]

  /**
    * Run elevator continuously
    * @return  unit
    */
  def run(): ZIO[Clock with Console, Nothing, Unit]
}

class ElevatorControlSystemImpl(
    val config: Configuration,
    val requests: Queue[Request],
    val tState: TRef[List[TRef[Elevator.State]]]
) extends ElevatorControlSystem {

  override def status(): ZIO[Any, Nothing, List[Elevator.State]] =
    STM.atomically {
      for {
        tstates <- tState.get
        states  <- STM.collectAll(tstates.map(_.get))
      } yield states
    }

  override def pickup(floor: Int, dir: Direction): ZIO[Any, NonEmptyList[ValidationError], Boolean] = {
    for {
      floor  <- Validations.floorNumber(config.totalFloors, floor).fold(ZIO.fail(_), ZIO.succeed(_))
      result <- requests.offer(PickupRequest(floor, dir))
    } yield result
  }

  override def drop(elevatorId: Int, floor: Int): ZIO[Any, NonEmptyList[ValidationError], Boolean] = {
    val dropReqValidation = (
      Validations.elevatorId(config.totalElevators - 1, elevatorId),
      Validations.floorNumber(config.totalFloors, floor)
    ).mapN(DropRequest)

    for {
      dropReq <- dropReqValidation.fold(ZIO.fail(_), ZIO.succeed(_))
      result  <- requests.offer(dropReq)
    } yield result
  }

  /**
    * Handling is done by updating the state of the elevator to the
    * new state computed after applying the request.
    * @param tState  Elevator state
    * @param req     Request (Pickup or Drop)
    * @return        STM transaction.
    */
  def handleRequest(tState: TRef[Elevator.State], req: Request): ZSTM[Any, Nothing, Elevator.State] =
    tState.modify { state =>
      Elevator.applyRequest(state, req) match {
        case None            => (STM.retry, state) // retry if req is not consumed.
        case Some(nextState) => (STM.succeed(nextState), nextState)
      }
    }.flatten

  /**
    * Move the elevator from one state to another forever.
    * @param config  App config
    * @param tState  State of elevator.
    * @return        ZIO effect.
    */
  def move(config: Configuration, tState: TRef[Elevator.State]): ZIO[Clock with Console, Nothing, Nothing] =
    (for {
      duration <-
        tState
          .modify {
            case i: Idle => STM.retry -> i // Do not transition in idle state.
            case other: Elevator.State =>
              STM.succeed(timeElapsed(config, other)) -> other
          }
          .flatten
          .commit

      _ <- ZIO.sleep(duration)

      // Move to next state after sleep
      _ <- (for {
          currState <- tState.get
          nextState = next(config.totalFloors, currState)
          _ <- tState.set(nextState)
        } yield ()).commit

    } yield ()).forever

  /**
    * Keep moving the elevators parallel forever.
    * Each elevator moves in separate fiber.
    * @return  ZIO
    */
  def moveElevatorsAsync(): ZIO[Clock with Console, Nothing, List[Fiber.Runtime[Nothing, Nothing]]] =
    for {
      tstates <- tState.get.commit
      fibers  <- ZIO.foreach(tstates)(move(config, _).fork)
    } yield fibers

  // assign requests forever
  def assignRequests(): URIO[Any, Fiber.Runtime[Nothing, Nothing]] =
    (for {
      req <- requests.take
      _ <- STM.atomically {
        for {
          tstates <- tState.get
          states  <- STM.foreach(tstates)(_.get)
          bestElevatorIndex = ElevatorControlSystem.findBestElevatorIndex(states)(req)
          _ <- handleRequest(tstates(bestElevatorIndex), req)
        } yield ()
      }
    } yield ()).forever

  // compose everything
  override def run(): ZIO[Clock with Console, Nothing, Unit] =
    for {
      elevatorFibers  <- moveElevatorsAsync()  // already runs in new fiber
      reqHandlerFiber <- assignRequests().fork // runs in new fiber
      _               <- Fiber.joinAll(elevatorFibers) <*> reqHandlerFiber.join
    } yield ()
}

object ElevatorControlSystem {

  // Initial state of each elevator.
  val initialState: Elevator.State = Elevator.State.Idle(Floor(0))

  def make(config: Configuration): ZIO[Any, Nothing, ElevatorControlSystemImpl] =
    for {
      requests <- Queue.unbounded[Request]
      ecs <- (for {
          tstates <- STM.collectAll(List.fill(config.totalElevators)(TRef.make(ElevatorControlSystem.initialState)))
          tState  <- TRef.make(tstates)
        } yield new ElevatorControlSystemImpl(config, requests, tState)).commit
    } yield ecs

  val nextFloor = (m: Moving, dir: Direction) => if (dir === Direction.Up) m.fromFloor + 1 else m.fromFloor - 1

  /**
    * Returns most preferably Idle elevator or else nearest elevator.
    * @param elevators State of elevators
    * @return          Nearest elevator or else Idle elevator index
    */
  def findBestElevatorIndex(elevators: List[Elevator.State]): Request => Int = {
    case Request.DropRequest(id, _) => id.value
    case Request.PickupRequest(floor, dir) =>
      elevators.zipWithIndex.minBy {
        case (i: Idle, _)                         => 0 -> math.abs(floor.num - i.floor.num)
        case (m: Moving, _) if m.dir === dir      => 1 -> math.abs(floor.num - nextFloor(m, dir).num)
        case (o: StopOnFloor, _) if o.dir === dir => 1 -> math.abs(floor.num - o.floor.num)
        case _                                    => 3 -> Int.MaxValue
      }._2
  }

}
