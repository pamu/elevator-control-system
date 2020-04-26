package com.ecs.domain

import cats.implicits._
import cats.data.ValidatedNel

sealed trait ValidationError extends Throwable
case class IntegerOutOfBounds(fieldName: String, upperBound: Int)
    extends Exception(s"$fieldName must be in range [0, $upperBound]")
    with ValidationError

object Validations {
  type Validation[A] = ValidatedNel[ValidationError, A]

  def integerInRange(
      fieldName: String,
      inclusiveUpperBound: Int,
      value: Int
  ): Validation[Int] =
    if (value >= 0 && value <= inclusiveUpperBound) value.validNel
    else IntegerOutOfBounds(fieldName, inclusiveUpperBound).invalidNel

  def floorNumber(totalFloors: Int, floor: Int): Validation[Floor] =
    integerInRange("Floor", totalFloors, floor).map(Floor(_))

  def elevatorId(maxElevatorIndex: Int, elevatorId: Int): Validation[ElevatorId] =
    integerInRange("Elevator id", maxElevatorIndex, elevatorId).map(ElevatorId(_))
}
