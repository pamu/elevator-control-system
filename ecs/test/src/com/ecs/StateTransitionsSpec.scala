package com.ecs

import com.ecs.domain.{Direction, Floor}
import com.ecs.logic.Elevator
import Elevator.State._
import Elevator._

class StateTransitionsSpec extends CommonSpec {

  "State transitions" must {

    "happen correctly" in {
      // Moving => Moving
      next(10, Moving(Direction.Up, Floor(0), Set())) mustBe
        Moving(Direction.Up, Floor(1), Set())

      // Moving => StopOnFloor
      next(10, Moving(Direction.Up, Floor(0), Set(Floor(1)))) mustBe
        StopOnFloor(Direction.Up, Floor(1), Set())

      // Moving => Idle
      next(10, Moving(Direction.Up, Floor(9), Set())) mustBe
        Idle(Floor(10))

      // Moving => StopOnFloor
      next(10, Moving(Direction.Up, Floor(9), Set(Floor(10)))) mustBe
        StopOnFloor(Direction.Up, Floor(10), Set())

      // Moving => Idle
      next(10, Moving(Direction.Down, Floor(1), Set())) mustBe
        Idle(Floor(0))

      // Moving => StopOnFloor
      next(10, Moving(Direction.Up, Floor(1), Set(Floor(2)))) mustBe
        StopOnFloor(Direction.Up, Floor(2), Set())

      // Idle => Idle
      next(10, Idle(Floor(1))) mustBe Idle(Floor(1))
    }
  }
}
