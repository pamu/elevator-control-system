package com.ecs

import com.ecs.domain.Request.{DropRequest, PickupRequest}
import com.ecs.domain.{Direction, ElevatorId, Floor}
import com.ecs.logic.Elevator.State._
import com.ecs.logic.ElevatorControlSystem

class BestElevatorSpec extends CommonSpec {

  "Elevator control system" must {

    "allocate idle elevator if found" in {
      Given("3 elevators")
      val elevators = List(
        Moving(Direction.Up, Floor(0), Set(Floor(1))),
        Idle(Floor(5)),
        StopOnFloor(Direction.Down, Floor(10), Set(Floor(0)))
      )

      When("Drop request for elevator 1 is sent")
      val dropReq = DropRequest(ElevatorId(1), Floor(10))
      Then("Elevator with index 1 is choosen")
      ElevatorControlSystem.findBestElevatorIndex(elevators)(dropReq) mustBe 1

      When("Pickup request is sent")
      val pickReq = PickupRequest(Floor(4), Direction.Up)
      ElevatorControlSystem.findBestElevatorIndex(elevators)(pickReq) mustBe 1
    }

    "allocate closest idle elevator" in {
      Given("All idle elevators")
      val elevators = List(
        Idle(Floor(1)),
        Idle(Floor(10)),
        Idle(Floor(3))
      )

      When("best elevator is requested")
      Then("nearest elevator is returned")
      ElevatorControlSystem.findBestElevatorIndex(elevators)(PickupRequest(Floor(1), Direction.Up)) mustBe 0
      ElevatorControlSystem.findBestElevatorIndex(elevators)(PickupRequest(Floor(10), Direction.Up)) mustBe 1
      ElevatorControlSystem.findBestElevatorIndex(elevators)(PickupRequest(Floor(3), Direction.Up)) mustBe 2

      ElevatorControlSystem.findBestElevatorIndex(elevators)(PickupRequest(Floor(4), Direction.Up)) mustBe 2
      ElevatorControlSystem.findBestElevatorIndex(elevators)(PickupRequest(Floor(5), Direction.Up)) mustBe 2
    }

    "allocate best elevator in moving and onfloor elevator" in {
      val elevators = List(
        Moving(Direction.Up, Floor(0), Set(Floor(10))),
        StopOnFloor(Direction.Down, Floor(10), Set(Floor(0)))
      )

      ElevatorControlSystem.findBestElevatorIndex(elevators)(PickupRequest(Floor(1), Direction.Up)) mustBe 0
      ElevatorControlSystem.findBestElevatorIndex(elevators)(PickupRequest(Floor(1), Direction.Down)) mustBe 1
    }

  }

}
