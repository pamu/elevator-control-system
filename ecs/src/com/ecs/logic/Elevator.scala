package com.ecs.logic

import zio.duration._
import com.ecs.domain.{Direction, Floor, Request}
import Elevator.State.{Idle, Moving, StopOnFloor}
import cats.implicits._
import com.ecs.Configuration
import com.ecs.domain.Request.{DropRequest, PickupRequest}

object Elevator {

  // Trait to add direction to case classes
  sealed trait Directed {
    val dir: Direction
    val isDirectedUp: Boolean   = dir === Direction.Up
    val isDirectedDown: Boolean = !isDirectedUp
  }

  // Elevator state ADT
  sealed abstract class State(val floor: Floor)

  object State {

    /**
      * Represents idle state of the elevator.
      * Note that elevator is idle state will be awaiting requests.
      * @param floor Floor in which elevator is idle.
      */
    case class Idle(override val floor: Floor) extends State(floor)

    /**
      * Represents a moving elevator.
      * @param dir       Direction of motion
      * @param fromFloor Floor from which elevator started moving.
      * @param stops     Set of upcoming floors to stop.
      */
    case class Moving(
        override val dir: Direction,
        fromFloor: Floor,
        stops: Set[Floor]
    ) extends State(fromFloor)
        with Directed {
      override val floor: Floor = fromFloor
    }

    /**
      * Represents stopped elevator on a floor for people to get-out or get-in.
      * @param dir   Direction in which elevator was moving before stop.
      * @param floor Floor on which elevator stopped.
      * @param stops Set of upcoming floors to stop.
      */
    case class StopOnFloor(
        override val dir: Direction, //directed towards
        override val floor: Floor,
        stops: Set[Floor]
    ) extends State(floor)
        with Directed

  }

  /**
    * State transition when drop request is handled.
    * Note that Idle state changes to moving in case floor in req is not same as Idle floor.
    * WARNING: None represents that request is not consumed. Some value represents
    * req is consumed successfully.
    * @param curr  Current elevator state.
    * @param req   Drop request.
    * @return      New state of the elevator after accepting the req.
    */
  def applyDropRequest(curr: Elevator.State, req: DropRequest): Option[Elevator.State] = {
    val targetFloor = req.floor
    curr match {
      case i: Idle if targetFloor === i.floor                          => i.some
      case i: Idle if targetFloor > i.floor                            => Moving(Direction.Up, i.floor, Set(targetFloor)).some
      case i: Idle if targetFloor < i.floor                            => Moving(Direction.Down, i.floor, Set(targetFloor)).some
      case m: Moving if targetFloor === m.fromFloor                    => none
      case m: Moving if m.isDirectedUp && targetFloor > m.fromFloor    => m.copy(stops = m.stops + targetFloor).some
      case m: Moving if m.isDirectedDown && targetFloor < m.fromFloor  => m.copy(stops = m.stops + targetFloor).some
      case o: StopOnFloor if targetFloor === o.floor                   => o.some
      case o: StopOnFloor if o.isDirectedUp && targetFloor > o.floor   => o.copy(stops = o.stops + targetFloor).some
      case o: StopOnFloor if o.isDirectedDown && targetFloor < o.floor => o.copy(stops = o.stops + targetFloor).some
      case _: Elevator.State                                           => none
    }
  }

  /**
    * State transition when pickup request is handled.
    * Note that Idle state changes to moving in case floor in req is not same as Idle floor.
    * WARNING: None represents that request is not consumed. Some value represents
    * req is consumed successfully.
    * @param curr  Current elevator state.
    * @param req   Pickup request.
    * @return      New state of the elevator after accepting the req.
    */
  def applyPickupRequest(curr: Elevator.State, req: PickupRequest): Option[Elevator.State] = {
    val reqFloor     = req.floor
    val reqDirection = req.direction
    curr match {
      case i: Idle if reqFloor === i.floor       => i.some
      case i: Idle if reqFloor > i.floor         => Moving(Direction.Up, i.floor, Set(reqFloor)).some
      case i: Idle                               => Moving(Direction.Down, i.floor, Set(reqFloor)).some
      case m: Moving if reqFloor === m.fromFloor => none
      case m: Moving if m.isDirectedUp && reqFloor > m.fromFloor && reqDirection.isUp =>
        m.copy(stops = m.stops + reqFloor).some
      case m: Moving if m.isDirectedDown && reqFloor < m.fromFloor && reqDirection.isDown =>
        m.copy(stops = m.stops + reqFloor).some
      case o: StopOnFloor if o.isDirectedUp && reqFloor >= o.floor && reqDirection.isUp =>
        o.copy(stops = o.stops + reqFloor).some
      case o: StopOnFloor if o.isDirectedDown && reqFloor <= o.floor && reqDirection.isDown =>
        o.copy(stops = o.stops + reqFloor).some
      case _: Elevator.State => none
    }
  }

  // Delegates to Pickup or Drop request based on req type.
  def applyRequest(
      curr: Elevator.State,
      req: Request
  ): Option[Elevator.State] =
    req match {
      case dr: DropRequest   => applyDropRequest(curr, dr)
      case pr: PickupRequest => applyPickupRequest(curr, pr)
    }

  /**
    * Gives time elapsed during the state transition.
    * @param config  Application configuration.
    * @param curr    Current state.
    * @return        Time elapsed
    */
  def timeElapsed(config: Configuration, curr: Elevator.State): Duration =
    (curr, next(config.totalFloors, curr)) match {
      case (_: Moving, _)      => config.oneFloorTravelDuration
      case (_: StopOnFloor, _) => config.waitingDuration
      case (_, _)              => 0.seconds
    }

  /**
    * Next state in FSM (Note that elevator is FSM)
    * @param totalFloors Total floors elevator is serving.
    * @param currState   Current state of the elevator.
    * @return            Next state of the elevator.
    */
  def next(totalFloors: Int, currState: Elevator.State): Elevator.State =
    currState match {
      case idle: Idle => idle
      case m @ Moving(dir, floor, stops) if m.isDirectedUp =>
        if (stops.contains(floor + 1)) StopOnFloor(dir, floor + 1, stops - (floor + 1))
        else if ((floor + 1).num === totalFloors) Idle(floor + 1)
        else Moving(dir, floor + 1, stops)
      case Moving(dir, floor, stops) =>
        if (stops.contains(floor - 1)) StopOnFloor(dir, floor - 1, stops - (floor - 1))
        else if ((floor - 1).num === 0) Idle(floor - 1)
        else Moving(dir, floor - 1, stops)
      case o: StopOnFloor if o.stops.isEmpty                      => Idle(o.floor)
      case o: StopOnFloor if o.floor.isTerminalFloor(totalFloors) => Idle(o.floor)
      case o: StopOnFloor                                         => Moving(o.dir, o.floor, o.stops)
    }

}
