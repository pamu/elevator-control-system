package com.ecs.domain

import cats.implicits._
import cats.Eq
import cats.Order

/**
 * Represents elevator id (Wrapper class)
 * @param value index of the elevator
 */
case class ElevatorId(value: Int) extends AnyVal

object ElevatorId {
  implicit val eq: Eq[ElevatorId] = Eq.fromUniversalEquals[ElevatorId]
}

/**
 *  Represents floor
 * @param num Floor number
 */
case class Floor(num: Int) extends AnyVal {
  def isTopFloor(totalFloors: Int): Boolean      = num === totalFloors
  def isGroundFloor: Boolean                     = num === 0
  def isTerminalFloor(totalFloors: Int): Boolean = isTopFloor(totalFloors) || isGroundFloor

  //noinspection ScalaStyle
  def +(n: Int): Floor = Floor(num + n)
  //noinspection ScalaStyle
  def -(n: Int): Floor = Floor(num - n)
}

object Floor {
  implicit val ord: Order[Floor] = cats.Order.by(_.num)
}

// Direction ADT
sealed abstract class Direction(val isUp: Boolean) {
  val isDown: Boolean = !isUp
}

object Direction {
  implicit val eq: Eq[Direction] = Eq.fromUniversalEquals[Direction]
  case object Up   extends Direction(true)
  case object Down extends Direction(false)
}

/**
 * Super type of elevator requests
 * @param floor Floor with which request is associated
 */
sealed abstract class Request(val floor: Floor)
object Request {

  /**
   * Represents Pickup request
   * @param floor     Floor from which pickup is request.
   * @param direction Direction in which requester as to go.
   */
  case class PickupRequest(override val floor: Floor, direction: Direction) extends Request(floor)

  /**
   *  Represents drop request.
   * @param id    Elevator id in which drop is requested.
   * @param floor Floor at which drop is requested.
   */
  case class DropRequest(id: ElevatorId, override val floor: Floor)         extends Request(floor)
}
