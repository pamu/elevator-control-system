package com.ecs

import cats.data.{NonEmptyList, Validated}
import com.ecs.domain.{ValidationError, Validations}
import com.ecs.domain.Validations.Validation

class ValidationSpec extends CommonSpec {

  "Validation spec" must {

    "ensure floor is in range [0, totalFloors]" in {
      Given("total floors 100")
      val totalFloors = 100
      When("floor value in range")
      Then("accept validation success")
      (0 to totalFloors).foreach { floorNum =>
        Validations
          .floorNumber(totalFloors, floorNum)
          .validValue()
          .num mustBe floorNum
      }
    }

    "ensure floor >= 0" in {
      Given("total floors 10")
      val totalFloors = 10
      When("floor value less than allowed lower bound")
      Then("validation fails")
      Validations
        .floorNumber(totalFloors, -1)
        .errors()
        .head
        .getMessage must include(s"Floor must be in range [0, $totalFloors]")
    }

    "ensure if floor <= totalFloors" in {
      Given("total floors 10")
      val totalFloors = 10
      When("floor value more than allowed upper bound")
      Then("validation fails")
      Validations
        .floorNumber(totalFloors, 11)
        .errors()
        .head
        .getMessage must include(s"Floor must be in range [0, $totalFloors]")
    }

    "ensure elevator id is in range [0, totalElevators - 1]" in {
      Given("total elevators 10")
      val totalElevators = 10
      val maxElevatorIndex = totalElevators - 1

      When("elevator id is lower bound")
      Then("validation passes")
      Validations
        .elevatorId(maxElevatorIndex, 0)
        .validValue()
        .value mustBe 0

      When("elevator id is in bounds")
      Then("validation passes")

      Validations.elevatorId(maxElevatorIndex, 4).validValue().value mustBe 4
      Validations.elevatorId(maxElevatorIndex, 9).validValue().value mustBe 9

      When("elevator id is below allowed lower bound")
      Then("validation fails")
      Validations.elevatorId(maxElevatorIndex, 10).errors().head.getMessage must include(
        s"Elevator id must be in range [0, 9]"
      )

      When("elevator is is above allowed upper bound")
      Then("validation fails")
      Validations.elevatorId(maxElevatorIndex, -1).errors().head.getMessage must include(
        s"Elevator id must be in range [0, 9]"
      )
    }

  }

  implicit class ValidationOps[A](value: Validation[A]) {

    def validValue(): A =
      value match {
        case Validated.Valid(a) => a
        case _                  => fail("Expected valid value, but found errors")
      }

    def errors(): NonEmptyList[ValidationError] =
      value match {
        case Validated.Invalid(errors) => errors
        case _                         => fail("Expected errors, but found valid value.")
      }
  }
}
