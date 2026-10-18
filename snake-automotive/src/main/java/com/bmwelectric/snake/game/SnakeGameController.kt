package com.bmwelectric.snake.game

const val BOARD_SIZE = 20
const val BEST_SCORE_KEY = "bmwSnakeBestScore"

enum class VehicleState {
    Parked,
    Driving,
}

enum class RoundState {
    Ready,
    Playing,
    Paused,
    GameOver,
    DrivingLocked,
}

enum class Direction {
    Up,
    Down,
    Left,
    Right,
}

enum class SpeedProfile(val label: String, val speedValue: String, val tickMillis: Long, val scoreStep: Int) {
    Eco("Eco", "01", 420L, 10),
    Comfort("Comfort", "02", 300L, 20),
    Dynamic("Dynamic", "03", 210L, 30),
}

data class Point(val x: Int, val y: Int)

data class SnakeGameState(
    val vehicleState: VehicleState = VehicleState.Parked,
    val roundState: RoundState = RoundState.Ready,
    val speedProfile: SpeedProfile = SpeedProfile.Comfort,
    val score: Int = 0,
    val bestScore: Int = 0,
    val direction: Direction = Direction.Right,
    val nextDirection: Direction = Direction.Right,
    val snake: List<Point> = initialSnake(),
    val energy: Point = Point(14, 10),
    val boardSize: Int = BOARD_SIZE,
    val statusAnnouncement: String = "Snake is ready",
) {
    val canPlay: Boolean = vehicleState == VehicleState.Parked
    val canStart: Boolean = canPlay && roundState == RoundState.Ready
    val canPauseOrContinue: Boolean = canPlay && (roundState == RoundState.Playing || roundState == RoundState.Paused)
    val canRestart: Boolean = canPlay && (roundState == RoundState.Paused || roundState == RoundState.GameOver)
    val canExit: Boolean = canPlay && (roundState == RoundState.Playing || roundState == RoundState.Paused || roundState == RoundState.GameOver)
    val canChangeSpeed: Boolean = canPlay && roundState == RoundState.Ready
    val canChangeDirection: Boolean = canPlay && roundState == RoundState.Playing
}

class SnakeGameController(
    bestScore: Int = 0,
    private val persistBestScore: (Int) -> Unit = {},
    private val nextEnergyProvider: (List<Point>, Point) -> Point = ::nextEnergyCell,
) {
    var state: SnakeGameState = SnakeGameState(bestScore = bestScore.coerceAtLeast(0))
        private set

    fun setVehicleState(vehicleState: VehicleState) {
        state = when (vehicleState) {
            VehicleState.Driving -> state.copy(
                vehicleState = VehicleState.Driving,
                roundState = RoundState.DrivingLocked,
                statusAnnouncement = "Driving. Game unavailable.",
            )
            VehicleState.Parked -> if (state.roundState == RoundState.DrivingLocked || state.vehicleState == VehicleState.Driving) {
                readyState(
                    bestScore = state.bestScore,
                    speedProfile = state.speedProfile,
                    announcement = "Parked. Game controls available.",
                )
            } else {
                state.copy(vehicleState = VehicleState.Parked)
            }
        }
    }

    fun setSpeedProfile(speedProfile: SpeedProfile) {
        if (state.canChangeSpeed) {
            state = state.copy(speedProfile = speedProfile)
        }
    }

    fun start() {
        if (!state.canStart) return
        state = newRoundState(
            bestScore = state.bestScore,
            speedProfile = state.speedProfile,
            announcement = "Snake started.",
        )
    }

    fun restart() {
        if (!state.canRestart) return
        state = newRoundState(
            bestScore = state.bestScore,
            speedProfile = state.speedProfile,
            announcement = "Snake restarted.",
        )
    }

    fun pauseOrContinue() {
        state = when {
            state.vehicleState != VehicleState.Parked -> state
            state.roundState == RoundState.Playing -> state.copy(
                roundState = RoundState.Paused,
                statusAnnouncement = "Paused.",
            )
            state.roundState == RoundState.Paused -> state.copy(
                roundState = RoundState.Playing,
                statusAnnouncement = "Snake continued.",
            )
            else -> state
        }
    }

    fun exit() {
        state = if (state.vehicleState == VehicleState.Driving) {
            state.copy(roundState = RoundState.DrivingLocked, statusAnnouncement = "Driving. Game unavailable.")
        } else {
            readyState(
                bestScore = state.bestScore,
                speedProfile = state.speedProfile,
                announcement = "Snake exited.",
            )
        }
    }

    fun inputDirection(direction: Direction) {
        if (!state.canChangeDirection || direction.isOpposite(state.direction)) return
        state = state.copy(nextDirection = direction, statusAnnouncement = "Direction ${direction.name}.")
    }

    fun tick() {
        if (state.roundState != RoundState.Playing || state.vehicleState != VehicleState.Parked) return

        val direction = state.nextDirection
        val head = state.snake.first().move(direction)
        val bodyWithoutTail = state.snake.dropLast(1)
        val hitWall = head.x !in 0 until state.boardSize || head.y !in 0 until state.boardSize
        val hitBody = head in bodyWithoutTail

        if (hitWall || hitBody) {
            state = state.copy(
                roundState = RoundState.GameOver,
                direction = direction,
                statusAnnouncement = "Collision. Game over. Score ${state.score}. Best ${state.bestScore}.",
            )
            return
        }

        val ateEnergy = head == state.energy
        val nextSnake = if (ateEnergy) listOf(head) + state.snake else listOf(head) + state.snake.dropLast(1)
        val nextScore = if (ateEnergy) state.score + state.speedProfile.scoreStep else state.score
        val nextBest = maxOf(state.bestScore, nextScore)
        if (nextBest != state.bestScore) {
            persistBestScore(nextBest)
        }

        state = state.copy(
            score = nextScore,
            bestScore = nextBest,
            direction = direction,
            nextDirection = direction,
            snake = nextSnake,
            energy = if (ateEnergy) nextEnergyProvider(nextSnake, state.energy) else state.energy,
            statusAnnouncement = if (ateEnergy) "Energy collected. Score $nextScore." else state.statusAnnouncement,
        )
    }
}

private fun readyState(
    bestScore: Int,
    speedProfile: SpeedProfile,
    announcement: String,
): SnakeGameState = SnakeGameState(
    vehicleState = VehicleState.Parked,
    roundState = RoundState.Ready,
    speedProfile = speedProfile,
    bestScore = bestScore,
    statusAnnouncement = announcement,
)

private fun newRoundState(
    bestScore: Int,
    speedProfile: SpeedProfile,
    announcement: String,
): SnakeGameState = SnakeGameState(
    vehicleState = VehicleState.Parked,
    roundState = RoundState.Playing,
    speedProfile = speedProfile,
    bestScore = bestScore,
    statusAnnouncement = announcement,
)

private fun initialSnake(): List<Point> = listOf(
    Point(6, 10),
    Point(5, 10),
    Point(4, 10),
    Point(3, 10),
)

private fun Point.move(direction: Direction): Point = when (direction) {
    Direction.Up -> copy(y = y - 1)
    Direction.Down -> copy(y = y + 1)
    Direction.Left -> copy(x = x - 1)
    Direction.Right -> copy(x = x + 1)
}

private fun Direction.isOpposite(other: Direction): Boolean = when (this) {
    Direction.Up -> other == Direction.Down
    Direction.Down -> other == Direction.Up
    Direction.Left -> other == Direction.Right
    Direction.Right -> other == Direction.Left
}

private fun nextEnergyCell(snake: List<Point>, current: Point): Point {
    val occupied = snake.toSet()
    val start = ((current.y * BOARD_SIZE) + current.x + 37).floorMod(BOARD_SIZE * BOARD_SIZE)
    for (offset in 0 until BOARD_SIZE * BOARD_SIZE) {
        val candidateIndex = (start + offset).floorMod(BOARD_SIZE * BOARD_SIZE)
        val candidate = Point(candidateIndex % BOARD_SIZE, candidateIndex / BOARD_SIZE)
        if (candidate !in occupied) return candidate
    }
    return current
}

private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor
