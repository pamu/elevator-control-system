package com.ecs

import com.ecs.logic.Elevator
import Elevator.State._
import Elevator._
import com.ecs.domain.{Direction, ElevatorId, Floor}
import com.ecs.domain.Request.{DropRequest, PickupRequest}

class RequestHandlerSpec extends CommonSpec {

  "Pickup request" must {
    "change state correctly" in {
      applyRequest(Idle(Floor(0)), PickupRequest(Floor(8), Direction.Up)) mustBe Some(
        Moving(Direction.Up, Floor(0), Set(Floor(8)))
      )

      applyRequest(
        Moving(Direction.Up, Floor(0), Set(Floor(10))),
        PickupRequest(
          Floor(0),
          Direction.Up
        )
      ) mustBe None

      applyRequest(
        Moving(Direction.Up, Floor(0), Set(Floor(10))),
        PickupRequest(
          Floor(6),
          Direction.Up
        )
      ) mustBe Some(Moving(Direction.Up, Floor(0), Set(Floor(6), Floor(10))))

      applyRequest(
        StopOnFloor(Direction.Up, Floor(5), Set(Floor(10))),
        PickupRequest(
          Floor(0),
          Direction.Up
        )
      ) mustBe None

      applyRequest(
        StopOnFloor(Direction.Up, Floor(5), Set(Floor(8))),
        PickupRequest(
          Floor(10),
          Direction.Up
        )
      ) mustBe Some(StopOnFloor(Direction.Up, Floor(5), Set(Floor(8), Floor(10))))
    }
  }

  "Drop request" must {
    "change state correctly" in {

      val elevator = ElevatorId(1)
      applyRequest(Idle(Floor(0)), DropRequest(elevator, Floor(8))) mustBe Some(
        Moving(Direction.Up, Floor(0), Set(Floor(8)))
      )

      applyRequest(
        Moving(Direction.Up, Floor(0), Set(Floor(10))),
        DropRequest(
          elevator,
          Floor(0)
        )
      ) mustBe None

      applyRequest(
        Moving(Direction.Up, Floor(0), Set(Floor(10))),
        DropRequest(
          elevator,
          Floor(6)
        )
      ) mustBe Some(Moving(Direction.Up, Floor(0), Set(Floor(6), Floor(10))))

      applyRequest(
        StopOnFloor(Direction.Up, Floor(5), Set(Floor(10))),
        DropRequest(
          elevator,
          Floor(0)
        )
      ) mustBe None

      applyRequest(
        StopOnFloor(Direction.Up, Floor(5), Set(Floor(8))),
        DropRequest(
          elevator,
          Floor(10)
        )
      ) mustBe Some(StopOnFloor(Direction.Up, Floor(5), Set(Floor(8), Floor(10))))

    }
  }
}
