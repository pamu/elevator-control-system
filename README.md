# Elevator control system

Elevator control system interface allows

1. Querying the state of elevators.
2. Stepping the simulation.
3. Sending Pickup/Drop requests to elevators in service.
4. Setting the initial configuration and state of the system including elevators.
5. Handles any number of elevators.

# Summary

1. ECS (Elevator control system) has  multiple elevators running in parallel (number of elevators depends on configuration value).
2. Each elevator uses [elevator algorithm](https://en.wikipedia.org/wiki/Elevator#Elevator) for serving requests.
3. There are two kinds of requests
    - PickupRequest
        - Any available elevator can handle this request.
        - ECS finds the suitable elevator for the request.
    - DropRequest
        - It is issued directly to particular elevator.
        - DropRequest has elevator id to which it is addressed.

    ```scala
    sealed abstract class Request(val floor: Floor)
    
    object Request {
      case class PickupRequest(override val floor: Floor, direction: Direction) extends Request(floor)
      case class DropRequest(id: ElevatorId, override val floor: Floor) extends Request(floor)
    }
    ```
4. ECS interface
    ```scala
    trait ElevatorControlSystem {
      def status(): ZIO[Any, Nothing, List[Elevator.State]]
      def pickup(floor: Int, dir: Direction): ZIO[Any, NonEmptyList[ValidationError], Boolean]
      def drop(elevatorId: Int, floor: Int): ZIO[Any, NonEmptyList[ValidationError], Boolean]
      def run(): ZIO[Clock with Console, Nothing, Unit]
    }
    ```
5. ECS uses STM (software transactional memory for safe state updates.)

# Libraries

1. ZIO Scala (STM and asynchronous programs)
2. Cats-core (type-safety and validations)
3. pureconfig (fail fast and typesafe configuration)
4. Scalatest (testing)

# Elevator algorithm

source: https://en.wikipedia.org/wiki/Elevator#Elevator_algorithm

The elevator algorithm, a simple algorithm by which a single elevator can decide where to stop, is summarised as follows:
- Continue travelling in the same direction while there are remaining requests in that same direction.
- If there are no further requests in that direction, then stop and become idle, or change direction if there are requests in the opposite direction.

# Design

### Elevator
1. Elevator is a Finite state machine (FSM).
2. Elevator goes from one state to another state with some delays.
3. Delays are used to simulate travelling time between floors.
4. Elevator states
    ```scala
    sealed trait State
    
      object State {
    
        case class Idle(floor: Floor) extends State
    
        case class Moving(
            override val dir: Direction,
            fromFloor: Floor,
            stops: Set[Floor]
        ) extends State
            with Directed
    
        case class StopOnFloor(
            override val dir: Direction, //directed towards
            floor: Floor,
            stops: Set[Floor]
        ) extends State
            with Directed
    
      }
    ```
 5. Each elevator simulation is run in separate fiber.
 6. A separate fiber is used to handle requests assigned to the elevator by the ECS.
 7. Configuration
 
     ```
    {
        total-floors = 10
        total-elevators = 3
        one-floor-travel-duration = 2 seconds
        waiting-duration = 1 second
    }
    ```
 ### Data-structures
 
 - Elevator state
```scala
TRef[Elevator.State]
```
- ECS state
```scala
TRef[List[TRef[Elevator.State]]]
```

# Code structure

```
.
├── Dockerfile
├── README.md
├── build.sc
├── ecs
│   ├── resources
│   │   └── application.conf
│   ├── src
│   │   └── com
│   │       └── ecs
│   │           ├── Configuration.scala
│   │           ├── ECSApp.scala
│   │           ├── domain
│   │           │   ├── Models.scala
│   │           │   └── Validations.scala
│   │           └── logic
│   │               ├── Elevator.scala
│   │               └── ElevatorControlSystem.scala
│   └── test
│       └── src
│           └── com
│               └── ecs
│                   ├── CommonSpec.scala
│                   ├── ElevatorControlSystemPureLogicSpec.scala
│                   ├── ElevatorControlSystemZIOLogicSpec.scala
│                   └── ValidationSpec.scala
├── mill
└── scalastyle_config.xml
```

# DataFlow

### Components
The main components are 
1. ECS
2. Elevator (Elevator is nothing but `TRef[Elevator.State]`)

### Flow
1. PickupRequests and DropRequests are sent to ECS.
2. ECS maintains pool of elevators.
3. ECS decides which elevator has to handle a particular request and 
assigns the request to appropriate elevator.
4. Finally, elevator handles request and updates state.


# Mill (alternative to SBT)

Uses `Mill` built tool. More about `Mill` built tool [here](http://www.lihaoyi.com/mill/).

# Compile, test and run

Note: `Mill` built tool is already present in the project root dir.

Note: All commands must be run project root dir.

### compile

```
./mill ecs.compile
```

### test

```
./mill ecs.test
```

### run

```
./mill ecs.run
```

# Docker

## Build

```
docker build -t ecs .
```

## Run

```
docker run -i ecs:latest ./mill ecs.run
```

# Output


